package com.demod.fbsr.map;

import static com.demod.fbsr.MapUtils.*;

import java.awt.geom.Rectangle2D;
import java.util.Collection;

import com.demod.fbsr.fp.FPBoundingBox;

public class MapRect3D {

	public static MapRect3D byFixedPoint(int x1, int x2, int y1, int y2, int height) {
		return new MapRect3D(x1, x2, y1, y2, height);
	}

	public static MapRect3D byUnit(double x1, double x2, double y1, double y2, double height) {
		return new MapRect3D(unitToFixedPoint(x1), unitToFixedPoint(x2), unitToFixedPoint(y1), unitToFixedPoint(y2),
				unitToFixedPoint(height));
	}

	public static MapRect3D byUnit(FPBoundingBox box, double height) {
		return byUnit(box.leftTop.x, box.leftTop.y, box.rightBottom.x, box.rightBottom.y, height);
	}

	public static MapRect3D byUnit(Rectangle2D.Double groundBounds, double height) {
		return byUnit(groundBounds.x, groundBounds.y, groundBounds.x + groundBounds.width,
				groundBounds.x + groundBounds.height, height);
	}

	public static MapRect3D combineAll(Collection<MapRect3D> rects) {
		if (rects.isEmpty()) {
			return new MapRect3D(0, 0, 0, 0, 0);
		}
		boolean first = true;
		int minX = 0, minY = 0, maxX = 0, maxY = 0, maxHeight = 0;
		for (MapRect3D rect : rects) {
			int x1 = rect.x1fp;
			int y1 = rect.y1fp;
			int x2 = rect.x2fp;
			int y2 = rect.y2fp;
			int height = rect.heightfp;
			if (first) {
				first = false;
				minX = x1;
				minY = y1;
				maxX = x2;
				maxY = y2;
				maxHeight = height;
			} else {
				minX = Math.min(minX, x1);
				minY = Math.min(minY, y1);
				maxX = Math.max(maxX, x2);
				maxY = Math.max(maxY, y2);
				maxHeight = Math.max(maxHeight, y2);
			}
		}
		return new MapRect3D(minX, minY, maxX, maxY, maxHeight);
	}

	final int x1fp;
	final int y1fp;
	final int x2fp;
	final int y2fp;
	final int heightfp;

	MapRect3D(int x1, int y1, int x2, int y2, int height) {
		this.x1fp = x1;
		this.y1fp = y1;
		this.x2fp = x2;
		this.y2fp = y2;
		this.heightfp = height;
	}

	public double getCenterX() {
		return fixedPointToUnit((x1fp + x2fp) / 2);
	}

	public double getCenterY() {
		return fixedPointToUnit((y1fp + y2fp) / 2);
	}

	public double getHeight() {
		return fixedPointToUnit(heightfp);
	}

	public int getHeightFP() {
		return heightfp;
	}

	public double getX1() {
		return fixedPointToUnit(x1fp);
	}

	public int getX1FP() {
		return x1fp;
	}

	public double getX2() {
		return fixedPointToUnit(x2fp);
	}

	public int getX2FP() {
		return x2fp;
	}

	public double getY1() {
		return fixedPointToUnit(y1fp);
	}

	public int getY1FP() {
		return y1fp;
	}

	public double getY2() {
		return fixedPointToUnit(y2fp);
	}

	public int getY2FP() {
		return y2fp;
	}

	public MapRect3D shiftUnit(double x, double y) {
		int fpX = unitToFixedPoint(x);
		int fpY = unitToFixedPoint(y);
		return new MapRect3D(x1fp + fpX, y1fp + fpY, x2fp + fpX, y2fp + fpY, heightfp);
	}

	public MapRect3D shiftHeightUnit(double height) {
		return new MapRect3D(x1fp, y1fp, x2fp, y2fp, this.heightfp + unitToFixedPoint(height));
	}

	public MapRect3D rotate90() {
		return new MapRect3D(-y2fp, x1fp, -y1fp, x2fp, heightfp);
	}

	public MapRect3D rotate180() {
		return new MapRect3D(-x2fp, -y2fp, -x1fp, -y1fp, heightfp);
	}

	public MapRect3D rotate270() {
		return new MapRect3D(y1fp, -x2fp, y2fp, -x1fp, heightfp);
	}

	public MapRect3D transformMatrix(double mx1, double mx2, double my1, double my2) {
		int dx1 = (int) (x1fp * mx1 + y1fp * mx2);
		int dy1 = (int) (x1fp * my1 + y1fp * my2);
		int dx2 = (int) (x2fp * mx1 + y2fp * mx2);
		int dy2 = (int) (x2fp * my1 + y2fp * my2);

		int minX = Math.min(dx1, dx2);
		int minY = Math.min(dy1, dy2);
		int maxX = Math.max(dx1, dx2);
		int maxY = Math.max(dy1, dy2);
		return new MapRect3D(minX, minY, maxX, maxY, heightfp);
	}

	public MapPosition getTopLeft() {
		return new MapPosition(x1fp, y1fp);
	}

	public MapPosition getTopRight() {
		return new MapPosition(x2fp, y1fp);
	}

	public MapPosition getBottomLeft() {
		return new MapPosition(x1fp, y2fp);
	}

	public MapPosition getBottomRight() {
		return new MapPosition(x2fp, y2fp);
	}

	public MapRect3D shift(MapPosition position) {
		return new MapRect3D(x1fp + position.xfp, y1fp + position.yfp, x2fp + position.xfp, y2fp + position.yfp, heightfp);
	}
}
