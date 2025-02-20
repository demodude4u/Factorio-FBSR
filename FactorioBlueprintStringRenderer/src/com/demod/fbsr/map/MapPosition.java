package com.demod.fbsr.map;

import static com.demod.fbsr.MapUtils.*;

import java.awt.Point;

public class MapPosition {

	// Fixed-point, 8-bit precision
	private final int x;
	private final int y;

	private MapPosition(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public static MapPosition byUnit(double x, double y) {
		return new MapPosition(unitToFixedPoint(x), unitToFixedPoint(y));
	}

	public static MapPosition byFixedPoint(int x, int y) {
		return new MapPosition(x, y);
	}

	public float getX() {
		return fixedPointToUnit(x);
	}

	public float getY() {
		return fixedPointToUnit(y);
	}

	public Point toPixels() {
		return new Point(fixedPointToPixels(x), fixedPointToPixels(y));
	}

	public MapPosition addUnit(double x, double y) {
		return new MapPosition(unitToFixedPoint(x), unitToFixedPoint(y));
	}

}
