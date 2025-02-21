package com.demod.fbsr.map;

import static com.demod.fbsr.MapUtils.*;

import java.awt.Rectangle;
import java.util.Collection;

public class MapRect {

	public static MapRect byFixedPoint(int x, int y, int width, int height) {
		return new MapRect(x, y, width, height);
	}

	public static MapRect byUnit(double x, double y, double width, double height) {
		return new MapRect(unitToFixedPoint(x), unitToFixedPoint(y), unitToFixedPoint(width), unitToFixedPoint(height));
	}

	public static MapRect byUnit(MapPosition pos, double width, double height) {
		return new MapRect(pos.x, pos.y, unitToFixedPoint(width), unitToFixedPoint(height));
	}

	public static MapRect combineAll(Collection<MapRect> rects) {
		if (rects.isEmpty()) {
			return new MapRect(0, 0, 0, 0);
		}
		boolean first = true;
		int minX = 0, minY = 0, maxX = 0, maxY = 0;
		for (MapRect rect : rects) {
			int x1 = rect.x;
			int y1 = rect.y;
			int x2 = rect.x + rect.width;
			int y2 = rect.y + rect.height;
			if (first) {
				first = false;
				minX = x1;
				minY = y1;
				maxX = x2;
				maxY = y2;
			} else {
				minX = Math.min(minX, x1);
				minY = Math.min(minY, y1);
				maxX = Math.max(maxX, x2);
				maxY = Math.max(maxY, y2);
			}
		}
		return new MapRect(minX, minY, maxX, maxY);
	}

	// Fixed-point, 8-bit precision
	final int x;
	final int y;
	final int width;
	final int height;

	MapRect(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public double getHeight() {
		return fixedPointToUnit(height);
	}

	public int getHeightFP() {
		return height;
	}

	public MapPosition getTopLeft() {
		return MapPosition.byFixedPoint(x, y);
	}

	public double getWidth() {
		return fixedPointToUnit(width);
	}

	public int getWidthFP() {
		return width;
	}

	public double getX() {
		return fixedPointToUnit(x);
	}

	public int getXFP() {
		return x;
	}

	public double getY() {
		return fixedPointToUnit(y);
	}

	public int getYFP() {
		return y;
	}

	public Rectangle toPixels() {
		return new Rectangle(fixedPointToPixels(x), fixedPointToPixels(y), fixedPointToPixels(width),
				fixedPointToPixels(height));
	}

	public MapRect add(MapPosition position) {
		return new MapRect(x + position.x, y + position.y, width, height);
	}

	public MapRect rotate90() {
		return new MapRect(-height - y, x, height, width);
	}

	public MapRect rotate180() {
		return new MapRect(-width - x, -height - y, width, height);
	}

	public MapRect rotate270() {
		return new MapRect(y, -width - x, height, width);
	}

	public MapRect transformMatrix(double mx1, double mx2, double my1, double my2) {
		int sx1 = x;
		int sy1 = y;
		int sx2 = x + width;
		int sy2 = y + height;

		int dx1 = (int) (sx1 * mx1 + sy1 * mx2);
		int dy1 = (int) (sx1 * my1 + sy1 * my2);
		int dx2 = (int) (sx2 * mx1 + sy2 * mx2);
		int dy2 = (int) (sx2 * my1 + sy2 * my2);

		int x = Math.min(dx1, dx2);
		int y = Math.min(dy1, dy2);
		int width = Math.max(dx1, dx2) - x;
		int height = Math.max(dy1, dy2) - y;
		return new MapRect(x, y, width, height);
	}
}
