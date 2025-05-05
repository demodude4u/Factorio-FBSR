package com.demod.fbsr;

import com.demod.fbsr.map.MapPosition;

public enum Dir16 {
	N, NNE, NE, ENE, //
	E, ESE, SE, SSE, //
	S, SSW, SW, WSW, //
	W, WNW, NW, NNW,//
	;

	public static final int[] FLIP_X = { 0, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1 };
	public static final int[] FLIP_Y = { 8, 7, 6, 5, 4, 3, 2, 1, 0, 15, 14, 13, 12, 11, 10, 9 };

	private final double angle;

	private Dir16() {
		final double[] QUARTER_ANGLES = { 0, Math.atan2(1, 2), Math.PI / 4.0, Math.atan2(2, 1) };
		angle = (ordinal() / 4) * (Math.PI / 2.0) + QUARTER_ANGLES[ordinal() % 4];
	}

	public Dir16 back() {
		return values()[((ordinal() + 8) % 16)];
	}

	public double getAngle() {
		return angle;
	}

	public Dir16 left() {
		return values()[(ordinal() + 12) % values().length];
	}

	public Dir16 right() {
		return values()[(ordinal() + 4) % values().length];
	}

	public Dir16 flipX() {
		return values()[FLIP_X[ordinal()]];
	}

	public Dir16 flipY() {
		return values()[FLIP_Y[ordinal()]];
	}

	public MapPosition offset(MapPosition pos, double distance) {
		double r = getAngle() - Math.PI / 2.0;
		return pos.addUnit(distance * Math.cos(r), distance * Math.sin(r));
	}

    public double getOrientation() {
		return ordinal() / 16.0;
    }
}
