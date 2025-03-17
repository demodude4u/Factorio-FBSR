package com.demod.fbsr.fp;

import java.awt.Color;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.factorio.fakelua.LuaValue;

public class FPColor {
	private static final Logger LOGGER = LoggerFactory.getLogger(FPColor.class);

	public final double r;
	public final double g;
	public final double b;
	public final double a;

	public FPColor(double r, double g, double b, double a) {
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;
	}

	public FPColor(LuaValue lua) {
		LuaValue luaR = lua.get("r");
		LuaValue luaG = lua.get("g");
		LuaValue luaB = lua.get("b");
		LuaValue luaA = lua.get("a");
		if (!luaR.isnil() || !luaG.isnil() || !luaB.isnil() || !luaA.isnil()) {
			r = luaR.optdouble(0);
			g = luaG.optdouble(0);
			b = luaB.optdouble(0);
			a = luaA.optdouble(isRange255(r, g, b, 0) ? 255 : 1);
		} else {
			r = lua.get(1).optdouble(0);
			g = lua.get(2).optdouble(0);
			b = lua.get(3).optdouble(0);
			a = lua.get(4).optdouble(isRange255(r, g, b, 0) ? 255 : 1);
		}
	}

	public static boolean isRange255(double r, double g, double b, double a) {
		return (r > 1) || (g > 1) || (b > 1) || (a > 1);
	}

	// Assuming pre-multiplied alpha
	public Color createColor() {
		if (isRange255(r, g, b, a)) { // 0 to 255
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
				if ((r / a) > 1 || (g / a) > 1 || (b / a) > 1) {
					LOGGER.warn("Premul color exceeds bounds! [{},{},{},{}]", r, g, b, a);
					return new Color((float) Math.min(1, (r / a)), (float) Math.min(1, (g / a)),
							(float) Math.min(1, (b / a)), (float) a);
				}
				return new Color((float) (r / a), (float) (g / a), (float) (b / a), (float) a);
			}
		}
	}

	// XXX docs say this is usually not the case, but can happen
	// XXX need to figure out if this is right, or the other way
	public Color createColorIgnorePreMultipliedAlpha() {
		if (r > 1 || g > 1 || b > 1 || a > 1) { // 0 to 255
			return new Color((int) r, (int) g, (int) b, (int) a);
		} else { // 0 to 1
			return new Color((float) r, (float) g, (float) b, (float) a);
		}
	}

}
