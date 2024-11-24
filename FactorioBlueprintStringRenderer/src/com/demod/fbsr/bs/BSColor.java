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

	// TODO create color was copied from lua color rules, need to verify

	// Assuming pre-multiplied alpha
	public Color createColor() {
		if (r > 1 || g > 1 || b > 1 || a > 1) { // 0 to 255
			if (a == 0) {
				return new Color(0, 0, 0, 0);
			} else if (a == 255) {
				return new Color((int) r, (int) g, (int) b, (int) a);
			} else {
				// Undo pre-multiplied alpha
				double div = a / 255;
				return new Color((int) (r / div), (int) (g / div), (int) (b / div), (int) a);
			}
		} else { // 0 to 1
			if (a == 0) {
				return new Color(0, 0, 0, 0);
			} else if (a == 1) {
				return new Color((float) r, (float) g, (float) b, (float) a);
			} else {
				// Undo pre-multiplied alpha
				return new Color((float) (r / a), (float) (g / a), (float) (b / a), (float) a);
			}
		}
	}

	// XXX lua docs say this is usually not the case, but can happen
	// XXX need to figure out if this is right, or the other way
	public Color createColorIgnorePreMultipliedAlpha() {
		if (r > 1 || g > 1 || b > 1 || a > 1) { // 0 to 255
			return new Color((int) r, (int) g, (int) b, (int) a);
		} else { // 0 to 1
			return new Color((float) r, (float) g, (float) b, (float) a);
		}
	}
}
