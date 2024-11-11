package com.demod.fbsr;

import java.awt.geom.Rectangle2D;

public class RectDef {
	private final double x;
	private final double y;
	private final double width;
	private final double height;

	public RectDef(double x, double y, double width, double height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public RectDef(Rectangle2D.Double r) {
		this(r.x, r.y, r.width, r.height);
	}

	public Rectangle2D.Double createRect() {
		return new Rectangle2D.Double(x, y, width, height);
	}
}
