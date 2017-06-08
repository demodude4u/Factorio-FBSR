package com.demod.fbsr;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

public abstract class PanelRenderer {
	protected double minWidth;
	protected double minHeight;

	public PanelRenderer(double minWidth, double minHeight) {
		this.minWidth = minWidth;
		this.minHeight = minHeight;
	}

	public double getMinHeight() {
		return minHeight;
	}

	public double getMinWidth() {
		return minWidth;
	}

	public abstract void render(Graphics2D g, Rectangle2D.Double bounds) throws Exception;
}
