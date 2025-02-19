package com.demod.fbsr.map;

import java.awt.Point;

import com.demod.fbsr.MapUtils;

public class MapPosition {

	// Fixed-point, 8-bit precision
	private final int x;
	private final int y;

	public MapPosition(float x, float y) {
		this.x = MapUtils.unitToFixedPoint(x);
		this.y = MapUtils.unitToFixedPoint(y);
	}

	public float getX() {
		return MapUtils.fixedPointToUnit(x);
	}

	public float getY() {
		return MapUtils.fixedPointToUnit(y);
	}

	public Point toPixels() {
		return new Point(MapUtils.fixedPointToPixels(x), MapUtils.fixedPointToPixels(y));
	}
}
