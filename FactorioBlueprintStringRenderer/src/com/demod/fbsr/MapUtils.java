package com.demod.fbsr;

public final class MapUtils {
	public static final int FRACTIONAL_BITS = 8;
	public static final int SCALING_FACTOR = (1 << FRACTIONAL_BITS);

	public static final int PIXELS_SHIFT = 2;

	public static int unitToFixedPoint(float unit) {
		return Math.round(unit * SCALING_FACTOR);
	}

	public static int unitToFixedPoint(double unit) {
		return (int) Math.round(unit * SCALING_FACTOR);
	}

	public static int fixedPointToPixels(int fp) {
		return fp >> PIXELS_SHIFT;
	}

	public static double fixedPointToUnit(int fp) {
		return fp / (double) SCALING_FACTOR;
	}

	public static int truncate(int fp) {
		return fp >> FRACTIONAL_BITS;
	}

}
