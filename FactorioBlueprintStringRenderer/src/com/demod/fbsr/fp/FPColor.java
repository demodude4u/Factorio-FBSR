package com.demod.fbsr.fp;

import java.awt.Color;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.factorio.fakelua.LuaValue;

public class FPColor {
	private static final Logger LOGGER = LoggerFactory.getLogger(FPColor.class);

	public final float r;
	public final float g;
	public final float b;
	public final float a;

	public FPColor(float r, float g, float b, float a) {
		if (isRange255(r, g, b, a)) {
			r /= 255f;
			g /= 255f;
			b /= 255f;
			a /= 255f;
		}
		
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;
	}

	public FPColor(float r, float g, float b) {
		if (isRange255(r, g, b, 0)) {
			r /= 255f;
			g /= 255f;
			b /= 255f;
		}
		
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = 1;
	}

	public FPColor(LuaValue lua) {
		float r = 0;
		float g = 0;
		float b = 0;
		float a = 1;
		boolean hasAlpha = false;
		
		if (lua.isarray()) {
			r = lua.get(1).checkfloat();
			g = lua.get(2).checkfloat();
			b = lua.get(3).checkfloat();
			if (lua.length() == 4) {
				a = lua.get(4).checkfloat();
				hasAlpha = true;
			}

		} else {
			r = lua.get("r").optfloat(0);
			g = lua.get("g").optfloat(0);
			b = lua.get("b").optfloat(0);
			LuaValue luaA = lua.get("a");
			if (!luaA.isnil()) {
				a = luaA.checkfloat();
				hasAlpha = true;
			}
		}
		
		if (isRange255(r, g, b, a)) {
			r /= 255;
			g /= 255;
			b /= 255;
			if (hasAlpha) {
				a /= 255;
			}
		}

		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;
	}

	public static boolean isRange255(double r, double g, double b, double a) {
		return (r > 1) || (g > 1) || (b > 1) || (a > 1);
	}

	// Assuming pre-multiplied alpha
	public Color createColor() {
		float r = (float) this.r;
		float g = (float) this.g;
		float b = (float) this.b;
		float a = (float) this.a;
		if (a == 0) {
			return new Color(0, 0, 0, 0);
		} else if (a == 1) {
			return new Color((float) r, (float) g, (float) b, (float) a);
		} else {
			// Undo pre-multiplied alpha
			if ((r / a) > 1 || (g / a) > 1 || (b / a) > 1) {
				// LOGGER.warn("Premul color exceeds bounds! [{},{},{},{}]", r, g, b, a);
				return new Color((float) Math.min(1, (r / a)), (float) Math.min(1, (g / a)),
						(float) Math.min(1, (b / a)), (float) a);
			}
			return new Color((float) (r / a), (float) (g / a), (float) (b / a), (float) a);
		}
	}

	// XXX docs say this is usually not the case, but can happen
	// XXX need to figure out if this is right, or the other way
	public Color createColorIgnorePreMultipliedAlpha() {
		return new Color((float) r, (float) g, (float) b, (float) a);
	}

}
