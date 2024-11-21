package com.demod.fbsr.bs;

import org.json.JSONObject;

public class BSColor {
	public final double r;
	public final double g;
	public final double b;
	public final double a;

	public BSColor(JSONObject json) {
		r = json.getDouble("r");
		g = json.getDouble("g");
		b = json.getDouble("b");
		a = json.getDouble("a");
	}
}
