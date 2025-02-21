package com.demod.fbsr.map;

import static com.demod.fbsr.MapUtils.*;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Collection;

import com.demod.fbsr.fp.FPVector;

public class MapPosition {

	public static MapPosition byFixedPoint(int x, int y) {
		return new MapPosition(x, y);
	}

	public static MapPosition byUnit(double x, double y) {
		return new MapPosition(unitToFixedPoint(x), unitToFixedPoint(y));
	}

	public static MapRect enclosingBounds(Collection<MapPosition> points) {
		if (points.isEmpty()) {
			return new MapRect(0, 0, 0, 0);
		}
		boolean first = true;
		int minX = 0, minY = 0, maxX = 0, maxY = 0;
		for (MapPosition point : points) {
			int x = point.x;
			int y = point.y;
			if (first) {
				first = false;
				minX = x;
				minY = y;
				maxX = x;
				maxY = y;
			} else {
				minX = Math.min(minX, x);
				minY = Math.min(minY, y);
				maxX = Math.max(maxX, x);
				maxY = Math.max(maxY, y);
			}
		}
		return new MapRect(minX, minY, maxX - minX, maxY - minY);
	}

	// Fixed-point, 8-bit precision
	final int x;
	final int y;

	MapPosition(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public MapPosition add(MapPosition position) {
		return new MapPosition(x + position.x, y + position.y);
	}

	public MapPosition addUnit(double x, double y) {
		return new MapPosition(unitToFixedPoint(x), unitToFixedPoint(y));
	}

	public MapPosition addUnit(FPVector vector) {
		return new MapPosition(unitToFixedPoint(vector.x), unitToFixedPoint(vector.y));
	}

	public Point2D.Double createPoint2D() {
		return new Point2D.Double(fixedPointToUnit(x), fixedPointToUnit(y));
	}

	public double getX() {
		return fixedPointToUnit(x);
	}

	public int getXCell() {
		return truncate(x);
	}

	public int getXFP() {
		return x;
	}

	public double getY() {
		return fixedPointToUnit(y);
	}

	public int getYCell() {
		return truncate(y);
	}

	public int getYFP() {
		return y;
	}

	public MapPosition multiplyUnit(double value) {
		return new MapPosition((int) (x * value), (int) (y * value));
	}

	public MapPosition multiplyUnitAdd(double value, MapPosition position) {
		return new MapPosition((int) (x * value) + position.x, (int) (y * value) + position.y);
	}

	public MapPosition rotate180() {
		return new MapPosition(-x, -y);
	}

	public MapPosition rotate270() {
		return new MapPosition(y, -x);
	}

	public MapPosition rotate90() {
		return new MapPosition(-y, x);
	}

	public Point toPixels() {
		return new Point(fixedPointToPixels(x), fixedPointToPixels(y));
	}

	public MapPosition transformMatrix(double mx1, double mx2, double my1, double my2) {
		return new MapPosition((int) (x * mx1 + y * mx2), (int) (x * my1 + y * my2));
	}

}
