package com.demod.fbsr.bs;

import org.json.JSONObject;

public class BSIcon {
	public final BSSignalID signal;
	public final int index;

	public BSIcon(JSONObject json) {
		signal = new BSSignalID(json.getJSONObject("signal"));
		index = json.getInt("index");
	}
}
