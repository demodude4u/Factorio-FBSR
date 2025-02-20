package com.demod.fbsr.map;

import static com.demod.fbsr.MapUtils.*;

import java.awt.Point;
import java.awt.geom.Point2D;

import com.demod.fbsr.fp.FPVector;

public class MapPosition {

	public static MapPosition byFixedPoint(int x, int y) {
		return new MapPosition(x, y);
	}

	public static MapPosition byUnit(double x, double y) {
		return new MapPosition(unitToFixedPoint(x), unitToFixedPoint(y));
	}

	// Fixed-point, 8-bit precision
	final int x;
	final int y;

	private MapPosition(int x, int y) {
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

	public int getXFP() {
		return x;
	}

	public int getXCell() {
		return truncate(x);
	}

	public double getY() {
		return fixedPointToUnit(y);
	}

	public int getYFP() {
		return y;
	}

	public int getYCell() {
		return truncate(y);
	}

	public MapPosition multiply(double value) {
		return new MapPosition((int) (x * value), (int) (y * value));
	}

	public MapPosition multiplyAdd(double value, MapPosition position) {
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
