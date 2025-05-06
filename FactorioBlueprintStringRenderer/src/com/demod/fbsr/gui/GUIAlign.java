package com.demod.fbsr.gui;

public enum GUIAlign {
	TOP_LEFT(0, 0), TOP_CENTER(0.5, 0), TOP_RIGHT(1, 0), CENTER_LEFT(0, 0.5), CENTER(0.5, 0.5),
	CENTER_RIGHT(1, 0.5), BOTTOM_LEFT(0, 1), BOTTOM_CENTER(0.5, 1), BOTTOM_RIGHT(1, 1);

	private final double horizontalFactor;
	private final double verticalFactor;

	GUIAlign(double horizontalFactor, double verticalFactor) {
		this.horizontalFactor = horizontalFactor;
		this.verticalFactor = verticalFactor;
	}

	public double getHorizontalFactor() {
		return horizontalFactor;
	}

	public double getVerticalFactor() {
		return verticalFactor;
	}
}
