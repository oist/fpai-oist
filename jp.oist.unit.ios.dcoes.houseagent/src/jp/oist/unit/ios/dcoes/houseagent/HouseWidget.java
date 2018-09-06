package jp.oist.unit.ios.dcoes.houseagent;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.flexiblepower.ui.Widget;

public class HouseWidget implements Widget {

	public static class Update {
		private final String houseId;
		private final double soc;
		private final String command;

		public Update(String houseId, double soc, String command) {
			super();
			this.houseId = houseId;
			this.soc = soc;
			this.command = command;
		}

		public String getHouseId() {
			return houseId;
		}

		public double getSoc() {
			return soc;
		}

		public String getCommand() {
			return command;
		}

	}

	private HouseModel model;

	@Override
	public String getTitle(Locale locale) {
		return "DCOES House";
	}

	public HouseWidget(HouseModel model) {
		this.model = model;
	}

	public Update update() {
		return new Update(model.getAgentId(), model.getSoC(),
				model.getCommand() == null ? "" : model.getCommand().toString());
	}

}
