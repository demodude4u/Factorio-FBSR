package com.demod.fbsr;

import java.awt.Graphics2D;

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

	public abstract void render(Graphics2D g, double width, double height) throws Exception;
}
