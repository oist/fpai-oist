package jp.oist.unit.ios.dcoes.monitor.message;

import org.json.JSONObject;

public interface IDcoesMessage {
	
	enum Type { ESS, WEATHER };
	
	IDcoesMessage.Type getType();

	JSONObject getMessage();
}