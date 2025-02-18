package com.demod.fbsr;

import java.awt.geom.Rectangle2D;

import com.demod.fbsr.fp.FPBoundingBox;

public class BoundingBoxWithHeight {
	public final double x1;
	public final double y1;
	public final double x2;
	public final double y2;
	public final double height;

	public BoundingBoxWithHeight(double x1, double y1, double x2, double y2, double height) {
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
		this.height = height;
	}

	public BoundingBoxWithHeight(FPBoundingBox box, double height) {
		x1 = box.leftTop.x;
		y1 = box.leftTop.y;
		x2 = box.rightBottom.x;
		y2 = box.rightBottom.y;
		this.height = height;
	}

	public BoundingBoxWithHeight(Rectangle2D.Double groundBounds, int height) {
		x1 = groundBounds.x;
		y1 = groundBounds.y;
		x2 = x1 + groundBounds.width;
		y2 = y1 + groundBounds.height;
		this.height = height;
	}

	public BoundingBoxWithHeight rotate(Direction direction) {
		Rectangle2D rotated = direction.rotateBounds(new Rectangle2D.Double(x1, y1, x2 - x1, y2 - y1));
		return new BoundingBoxWithHeight(rotated.getMinX(), rotated.getMinY(), rotated.getMaxX(), rotated.getMaxY(),
				height);
	}

	public BoundingBoxWithHeight shift(double x, double y) {
		return new BoundingBoxWithHeight(x1 + x, y1 + y, x2 + x, y2 + y, height);
	}

	public double getCenterX() {
		return (x1 + x2) / 2.0;
	}

	public double getCenterY() {
		return (y1 + y2) / 2.0 - height / 2.0;
	}
}
