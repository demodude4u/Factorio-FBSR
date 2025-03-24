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
		return new MapRect(pos.xfp, pos.yfp, unitToFixedPoint(width), unitToFixedPoint(height));
	}

	public static MapRect combineAll(Collection<MapRect> rects) {
		if (rects.isEmpty()) {
			return new MapRect(0, 0, 0, 0);
		}
		boolean first = true;
		int minX = 0, minY = 0, maxX = 0, maxY = 0;
		for (MapRect rect : rects) {
			int x1 = rect.xfp;
			int y1 = rect.yfp;
			int x2 = rect.xfp + rect.widthfp;
			int y2 = rect.yfp + rect.heightfp;
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
		return new MapRect(minX, minY, maxX - minX, maxY - minY);
	}

	// Fixed-point, 8-bit precision
	final int xfp;
	final int yfp;
	final int widthfp;
	final int heightfp;

	MapRect(int x, int y, int width, int height) {
		this.xfp = x;
		this.yfp = y;
		this.widthfp = width;
		this.heightfp = height;
	}

	public MapRect add(MapPosition position) {
		return new MapRect(xfp + position.xfp, yfp + position.yfp, widthfp, heightfp);
	}

	public MapRect expandUnit(double distance) {
		int distFP = unitToFixedPoint(distance);
		return new MapRect(xfp - distFP, yfp - distFP, widthfp + distFP * 2, heightfp + distFP * 2);
	}

	public double getHeight() {
		return fixedPointToUnit(heightfp);
	}

	public int getHeightFP() {
		return heightfp;
	}

	public MapPosition getTopLeft() {
		return MapPosition.byFixedPoint(xfp, yfp);
	}

	public double getWidth() {
		return fixedPointToUnit(widthfp);
	}

	public int getWidthFP() {
		return widthfp;
	}

	public double getX() {
		return fixedPointToUnit(xfp);
	}

	public int getXFP() {
		return xfp;
	}

	public double getY() {
		return fixedPointToUnit(yfp);
	}

	public int getYFP() {
		return yfp;
	}

	public MapRect rotate180() {
		return new MapRect(-widthfp - xfp, -heightfp - yfp, widthfp, heightfp);
	}

	public MapRect rotate270() {
		return new MapRect(yfp, -widthfp - xfp, heightfp, widthfp);
	}

	public MapRect rotate90() {
		return new MapRect(-heightfp - yfp, xfp, heightfp, widthfp);
	}

	public Rectangle toPixels() {
		return new Rectangle(fixedPointToPixels(xfp), fixedPointToPixels(yfp), fixedPointToPixels(widthfp),
				fixedPointToPixels(heightfp));
	}

	public MapRect transformMatrix(double mx1, double mx2, double my1, double my2) {
		int sx1 = xfp;
		int sy1 = yfp;
		int sx2 = xfp + widthfp;
		int sy2 = yfp + heightfp;

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

	public MapRect scale(double scale) {
		int hw = widthfp / 2;
		int hh = heightfp / 2;
		int sw = (int) (widthfp * scale);
		int sh = (int) (heightfp * scale);
		int shw = sw / 2;
		int shh = sh / 2;
		return new MapRect(xfp + hw - shw, yfp + hh - shh, sw, sh);
	}

	public MapRect addUnit(double x, double y) {
		return new MapRect(xfp + unitToFixedPoint(x), yfp + unitToFixedPoint(y), widthfp, heightfp);
	}
}
