package com.demod.fbsr.bs;

import org.json.JSONObject;

public class BSNetworkPorts {
	public final boolean red;
	public final boolean green;

	public BSNetworkPorts(JSONObject json) {
		red = json.optBoolean("red");
		green = json.optBoolean("green");
	}
}
