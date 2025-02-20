package com.demod.fbsr.map;

import static com.demod.fbsr.MapUtils.*;

import java.awt.geom.Rectangle2D;

import com.demod.fbsr.Direction;
import com.demod.fbsr.fp.FPBoundingBox;

public class MapRect3D {
	private final int x1;
	private final int y1;
	private final int x2;
	private final int y2;
	private final int height;

	private MapRect3D(int x1, int y1, int x2, int y2, int height) {
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
		this.height = height;
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

	public MapRect3D rotate(Direction direction) {
		Rectangle2D rotated = direction.rotateBounds(new Rectangle2D.Double(x1, y1, x2 - x1, y2 - y1));
		return new MapRect3D((int) rotated.getMinX(), (int) rotated.getMinY(), (int) rotated.getMaxX(),
				(int) rotated.getMaxY(), height);
	}

	public MapRect3D shift(double x, double y) {
		int fpX = unitToFixedPoint(x);
		int fpY = unitToFixedPoint(y);
		return new MapRect3D(x1 + fpX, y1 + fpY, x2 + fpX, y2 + fpY, height);
	}

	public double getCenterX() {
		return fixedPointToUnit(x1 + x2) / 2;
	}

	public double getCenterY() {
		return fixedPointToUnit(y1 + y2) / 2;
	}

	public MapRect3D shiftHeight(double height) {
		return new MapRect3D(x1, y1, x2, y2, this.height + unitToFixedPoint(height));
	}
}
