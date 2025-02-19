package com.demod.fbsr;

public final class MapUtils {
	public static final int FRACTIONAL_BITS = 8;
	public static final int SCALING_FACTOR = (1 << FRACTIONAL_BITS);

	public static final int PIXELS_SHIFT = 3;

	public static int unitToFixedPoint(float unit) {
		return Math.round(unit * SCALING_FACTOR);
	}

	public static int fixedPointToPixels(int fp) {
		return fp >> PIXELS_SHIFT;
	}

	public static float fixedPointToUnit(int fp) {
		return fp / (float) SCALING_FACTOR;
	}

}
