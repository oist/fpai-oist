package jp.oist.unit.ios.dcoes.houseagent;

import java.util.Hashtable;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;

import javax.measure.Measure;
import javax.measure.unit.SI;

import org.flexiblepower.context.FlexiblePowerContext;
import org.flexiblepower.ui.Widget;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta;
import net.powermatcher.api.AgentEndpoint;
import net.powermatcher.api.monitoring.ObservableAgent;

@Component(designateFactory = HouseModel.Config.class)
public class HouseModel {

	@Meta.OCD
	public interface Config {
		@Meta.AD(deflt = "house", description = "Agent ID for the house")
		String agentId();

		@Meta.AD(deflt = "auctioneer", description = "Agent ID for the desired parent")
		String desiredParentId();

		@Meta.AD(deflt = "62", description = "Initial state of charge for the battery (between 20 and 95)")
		int initialSoC();

		@Meta.AD(deflt = "700", description = "The power when electricity is exchanged with neighbours")
		double exchangeRateWatt();
	}

	private final static Logger LOG = LoggerFactory.getLogger(HouseModel.class);

	private Config config;
	private ServiceRegistration<?> agentServiceRegistration;
	private FlexiblePowerContext context;
	private double soc;
	private Random random;
	private HouseAgent agent;
	private ScheduledFuture<?> socTask;

	private HouseCommand command;

	private ServiceRegistration<Widget> widgetServiceRegistration;

	@Activate
	public void activate(BundleContext bundleContext, Map<String, Object> properties) {
		config = Configurable.createConfigurable(HouseModel.Config.class, properties);

		agent = new HouseAgent(this, config.exchangeRateWatt());
		agent.init(config.agentId(), config.desiredParentId());

		agentServiceRegistration = bundleContext.registerService(
				new String[] { AgentEndpoint.class.getName(), ObservableAgent.class.getName() }, agent,
				new Hashtable<>());
		random = new Random();

		soc = config.initialSoC();

		socTask = context.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				updateSoC();

			}
		}, Measure.zero(SI.SECOND), Measure.valueOf(10, SI.SECOND));

		// Register the widget
		widgetServiceRegistration = bundleContext.registerService(Widget.class, new HouseWidget(this),
				new Hashtable<>());
	}

	@Deactivate
	public void deactivate() {
		socTask.cancel(false);
		agentServiceRegistration.unregister();
		widgetServiceRegistration.unregister();
	}

	@Reference(optional = false, dynamic = false, multiple = false)
	public void setContext(FlexiblePowerContext context) {
		this.context = context;
	}

	private void updateSoC() {
		// TODO use real SoC
		double newSoc = soc + (random.nextDouble() * 2) - 1;
		soc = Math.max(20, Math.min(95, newSoc));
		agent.notifyNewSoc(soc);
		LOG.info("The new SoC for house " + config.agentId() + " is " + soc + "%");
	}

	public void setCommand(HouseCommand command) {
		this.command = command;
		LOG.info("The command for house " + config.agentId() + " is " + command.toString());
		// TODO actually do something!
	}

	public String getAgentId() {
		return config.agentId();
	}

	public double getSoC() {
		return soc;
	}

	public HouseCommand getCommand() {
		return command;
	}

}
