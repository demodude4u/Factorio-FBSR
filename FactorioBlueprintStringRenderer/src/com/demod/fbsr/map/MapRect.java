package com.demod.fbsr.map;

import static com.demod.fbsr.MapUtils.*;

import java.awt.Rectangle;

public class MapRect {
	public static final int FRACTIONAL_BITS = 8;
	public static final int SCALING_FACTOR = (1 << FRACTIONAL_BITS);

	public static final int PIXELS_SHIFT = 3;

	// Fixed-point, 8-bit precision
	private final int x;
	private final int y;
	private final int width;
	private final int height;

	private MapRect(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public static MapRect byUnit(double x, double y, double width, double height) {
		return new MapRect(unitToFixedPoint(x), unitToFixedPoint(y), unitToFixedPoint(width), unitToFixedPoint(height));
	}

	public static MapRect byFixedPoint(int x, int y, int width, int height) {
		return new MapRect(x, y, width, height);
	}

	public float getX() {
		return fixedPointToUnit(x);
	}

	public float getY() {
		return fixedPointToUnit(y);
	}

	public float getWidth() {
		return fixedPointToUnit(width);
	}

	public float getHeight() {
		return fixedPointToUnit(height);
	}

	public Rectangle toPixels() {
		return new Rectangle(fixedPointToPixels(x), fixedPointToPixels(y), fixedPointToPixels(width),
				fixedPointToPixels(height));
	}

	public MapPosition getTopLeft() {
		return MapPosition.byFixedPoint(x, y);
	}
}
