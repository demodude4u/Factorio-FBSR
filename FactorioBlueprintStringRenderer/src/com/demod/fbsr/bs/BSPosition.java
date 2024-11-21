package com.demod.fbsr.bs;

import org.json.JSONObject;

public class BSPosition {
	public final double x;
	public final double y;

	public BSPosition(JSONObject json) {
		x = json.getDouble("x");
		y = json.getDouble("y");
	}
}
