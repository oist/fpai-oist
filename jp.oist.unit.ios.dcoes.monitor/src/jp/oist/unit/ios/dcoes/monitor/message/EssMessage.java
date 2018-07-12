package jp.oist.unit.ios.dcoes.monitor.message;

import org.json.JSONObject;

public class EssMessage implements IDcoesMessage {
	
	private JSONObject message;
	
	public EssMessage(JSONObject message) {
		this.message = message;
	}

	@Override
	public JSONObject getMessage() {
		return message;
	}

	@Override
	public Type getType() {
		return IDcoesMessage.Type.ESS;
	}
}