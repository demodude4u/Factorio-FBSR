package com.demod.fbsr.map;

import static com.demod.fbsr.MapUtils.*;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Collection;

import com.demod.fbsr.MapUtils;
import com.demod.fbsr.fp.FPVector;

public class MapPosition {

	public static MapPosition byFixedPoint(int x, int y) {
		return new MapPosition(x, y);
	}

	public static MapPosition byUnit(double x, double y) {
		return new MapPosition(unitToFixedPoint(x), unitToFixedPoint(y));
	}

	public static MapPosition convert(FPVector v) {
		return byUnit(v.x, v.y);
	}

	public static MapPosition convert(Point2D.Double p) {
		return byUnit(p.x, p.y);
	}

	public static MapRect enclosingBounds(Collection<MapPosition> points) {
		if (points.isEmpty()) {
			return new MapRect(0, 0, 0, 0);
		}
		boolean first = true;
		int minX = 0, minY = 0, maxX = 0, maxY = 0;
		for (MapPosition point : points) {
			int x = point.xfp;
			int y = point.yfp;
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
	final int xfp;

	final int yfp;

	MapPosition(int x, int y) {
		this.xfp = x;
		this.yfp = y;
	}

	public MapPosition add(MapPosition position) {
		return new MapPosition(xfp + position.xfp, yfp + position.yfp);
	}

	public MapPosition addUnit(double x, double y) {
		return new MapPosition(xfp + unitToFixedPoint(x), yfp + unitToFixedPoint(y));
	}

	public MapPosition addUnit(FPVector vector) {
		return new MapPosition(xfp + unitToFixedPoint(vector.x), yfp + unitToFixedPoint(vector.y));
	}

	public Point2D.Double createPoint2D() {
		return new Point2D.Double(fixedPointToUnit(xfp), fixedPointToUnit(yfp));
	}

	public double getX() {
		return fixedPointToUnit(xfp);
	}

	public int getXCell() {
		return softTruncate(xfp);
	}

	public int getXFP() {
		return xfp;
	}

	public double getY() {
		return fixedPointToUnit(yfp);
	}

	public int getYCell() {
		return softTruncate(yfp);
	}

	public int getYFP() {
		return yfp;
	}

	public MapPosition multiplyUnit(double value) {
		return new MapPosition((int) (xfp * value), (int) (yfp * value));
	}

	public MapPosition multiplyUnit(double x, double y) {
		return new MapPosition((int) (this.xfp * x), (int) (this.yfp * y));
	}

	public MapPosition multiplyUnitAdd(double value, MapPosition position) {
		return new MapPosition((int) (xfp * value) + position.xfp, (int) (yfp * value) + position.yfp);
	}

	public MapPosition rotate180() {
		return new MapPosition(-xfp, -yfp);
	}

	public MapPosition rotate270() {
		return new MapPosition(yfp, -xfp);
	}

	public MapPosition rotate90() {
		return new MapPosition(-yfp, xfp);
	}

	public MapPosition sub(MapPosition position) {
		return new MapPosition(xfp - position.xfp, yfp - position.yfp);
	}

	public Point toPixels() {
		return new Point(fixedPointToPixels(xfp), fixedPointToPixels(yfp));
	}

	public MapPosition transformMatrix(double mx1, double mx2, double my1, double my2) {
		return new MapPosition((int) (xfp * mx1 + yfp * mx2), (int) (xfp * my1 + yfp * my2));
	}

	public int getXHalfCell() {
		return xfp >> (MapUtils.FRACTIONAL_BITS - 1);
	}

	public int getYHalfCell() {
		return yfp >> (MapUtils.FRACTIONAL_BITS - 1);
	}

	public MapPosition flipX() {
		return new MapPosition(-xfp, yfp);
	}

	public MapPosition flipY() {
		return new MapPosition(xfp, -yfp);
	}

	public double distance(MapPosition other) {
		int dxfp = other.xfp - xfp;
		int dyfp = other.yfp - yfp;
		return MapUtils.fixedPointToUnit((int) Math.round(Math.sqrt(dxfp * dxfp + dyfp * dyfp)));
	}

	public MapPosition subtract(MapPosition position) {
		return new MapPosition(xfp - position.xfp, yfp - position.yfp);
	}

	public static MapPosition average(MapPosition p1, MapPosition p2) {
		return new MapPosition((p1.xfp + p2.xfp) / 2, (p1.yfp + p2.yfp) / 2);
	}
}
