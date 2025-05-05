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

	public static int hardTruncate(int fp) {
		return fp >> FRACTIONAL_BITS;
	}

	public static int softTruncate(int fp) {
		int ret = fp >> FRACTIONAL_BITS;
		int subState = (fp >> (FRACTIONAL_BITS - 3)) & 0b111;
		// Round up if .875 or higher to the next number
		return (subState == 0b111) ? ret + 1 : ret;
	}

}
