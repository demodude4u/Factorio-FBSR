package com.demod.fbsr.bs;

import org.json.JSONObject;

public class BSTile {
	public final BSPosition position;
	public final String name;

	public BSTile(JSONObject json) {
		position = new BSPosition(json.getJSONObject("position"));
		name = json.getString("name");
	}
}
