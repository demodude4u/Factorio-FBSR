package com.demod.fbsr.map;

import java.awt.Rectangle;

import com.demod.fbsr.MapUtils;

public class MapRect {
	public static final int FRACTIONAL_BITS = 8;
	public static final int SCALING_FACTOR = (1 << FRACTIONAL_BITS);

	public static final int PIXELS_SHIFT = 3;

	// Fixed-point, 8-bit precision
	private final int x;
	private final int y;
	private final int width;
	private final int height;

	public MapRect(float x, float y, float width, float height) {
		this.x = MapUtils.unitToFixedPoint(x);
		this.y = MapUtils.unitToFixedPoint(y);
		this.width = MapUtils.unitToFixedPoint(width);
		this.height = MapUtils.unitToFixedPoint(height);
	}

	public float getX() {
		return x / (float) SCALING_FACTOR;
	}

	public float getY() {
		return y / (float) SCALING_FACTOR;
	}

	public Rectangle toPixels() {
		return new Rectangle(x >> PIXELS_SHIFT, y >> PIXELS_SHIFT, width >> PIXELS_SHIFT, height >> PIXELS_SHIFT);
	}
}
