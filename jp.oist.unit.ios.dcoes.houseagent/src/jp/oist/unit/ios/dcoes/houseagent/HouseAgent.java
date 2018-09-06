package jp.oist.unit.ios.dcoes.houseagent;

import net.powermatcher.api.AgentEndpoint;
import net.powermatcher.api.data.Bid;
import net.powermatcher.api.data.MarketBasis;
import net.powermatcher.api.data.PointBidBuilder;
import net.powermatcher.api.messages.BidUpdate;
import net.powermatcher.api.messages.PriceUpdate;
import net.powermatcher.core.BaseAgentEndpoint;

public class HouseAgent extends BaseAgentEndpoint implements AgentEndpoint {

	private double soc;
	private final HouseModel houseModel;
	private final double exchangeRateWatt;
	private BidUpdate lastBidUpdate;

	public HouseAgent(HouseModel houseModel, double exchangeRateWatt) {
		this.houseModel = houseModel;
		this.exchangeRateWatt = exchangeRateWatt;
	}

	@Override
	protected void init(String agentId, String desiredParentId) {
		super.init(agentId, desiredParentId);
	}

	@Override
	public void handlePriceUpdate(PriceUpdate priceUpdate) {
		super.handlePriceUpdate(priceUpdate);
		BidUpdate lastBidUpdate = this.lastBidUpdate;
		if (lastBidUpdate == null || priceUpdate.getBidNumber() != lastBidUpdate.getBidNumber()) {
			// This price is NOT based on our last bid, ignore!
			return;
		}
		// What did we promise in our last bid?
		double promisedPowerValue = lastBidUpdate.getBid().getDemandAt(priceUpdate.getPrice());
		if (promisedPowerValue > 1) {
			houseModel.setCommand(HouseCommand.RECEIVE);
		} else if (promisedPowerValue < -1) {
			houseModel.setCommand(HouseCommand.PROVIDE);
		} else {
			houseModel.setCommand(HouseCommand.NO_EXCHANGE);
		}
	}

	public void notifyNewSoc(double soc) {
		this.soc = soc;
		net.powermatcher.api.AgentEndpoint.Status status = this.getStatus();
		if (status.isConnected()) {
			Bid newBid = constructBid(status.getMarketBasis());
			lastBidUpdate = publishBid(newBid);
		}
	}

	private Bid constructBid(MarketBasis marketBasis) {
		double normalizedSoC = (soc - 20d) / (95d - 20d);
		if (normalizedSoC < 0) {
			// I NEED energy
			return Bid.flatDemand(marketBasis, exchangeRateWatt);
		} else if (normalizedSoC < 0.2) { // 0 <= normalizedSoC < 0.2
			// I can receive energy
			double cutoffPriceReceiving = transformPrice(marketBasis, (1d - normalizedSoC) / 2d + 0.5);
			return new PointBidBuilder(marketBasis).add(cutoffPriceReceiving, exchangeRateWatt)
					.add(cutoffPriceReceiving, 0).build();
		} else if (normalizedSoC < 0.8) { // 0.2 <= normalizedSoC < 0.8
			// I can receive or provide energy
			double cutoffPriceReceiving = transformPrice(marketBasis, (1d - normalizedSoC) / 2d);
			double cutoffPriceProviding = transformPrice(marketBasis, cutoffPriceReceiving + 0.5);
			return new PointBidBuilder(marketBasis).add(cutoffPriceReceiving, exchangeRateWatt)
					.add(cutoffPriceReceiving, 0).add(cutoffPriceProviding, 0)
					.add(cutoffPriceProviding, -exchangeRateWatt).build();
		} else if (normalizedSoC < 1) { // 0.8 <= normalizedSoC < 1
			// I can provide energy
			double cutoffPriceProviding = transformPrice(marketBasis, (1d - normalizedSoC) / 2d);
			return new PointBidBuilder(marketBasis).add(cutoffPriceProviding, 0)
					.add(cutoffPriceProviding, -exchangeRateWatt).build();
		} else { // normalizedSoC >= 1
			// I NEED to get rid of energy
			return Bid.flatDemand(marketBasis, -exchangeRateWatt);
		}
	}

	private double transformPrice(MarketBasis marketBasis, double priceBetweenZeroAndOne) {
		return marketBasis.getMinimumPrice()
				+ (priceBetweenZeroAndOne * (marketBasis.getMaximumPrice() - marketBasis.getMinimumPrice()));
	}

}
