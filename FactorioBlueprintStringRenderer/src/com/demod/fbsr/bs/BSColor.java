package com.demod.fbsr.bs;

import java.awt.Color;

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

	public static JSONObject toJson(Color color) {
		JSONObject json = new JSONObject();
		json.put("r", color.getRed());
		json.put("g", color.getGreen());
		json.put("b", color.getBlue());
		json.put("a", color.getAlpha());
		return json;
	}

	// Assuming NOT pre-multiplied alpha
	public Color createColor() {
		if (r > 1 || g > 1 || b > 1 || a > 1) { // 0 to 255
			if (a == 0) {
				return new Color(0, 0, 0, 0);
			} else {
				return new Color((int) r, (int) g, (int) b, (int) a);
			}
		} else { // 0 to 1
			if (a == 0) {
				return new Color(0, 0, 0, 0);
			} else {
				return new Color((float) r, (float) g, (float) b, (float) a);
			}
		}
	}
}
