package com.demod.fbsr;

import java.awt.image.BufferedImage;

public class RenderResult {
	public final BufferedImage image;
	public final long renderTime;
	public final double renderScale;

	public RenderResult(BufferedImage image, long renderTime, double renderScale) {
		this.image = image;
		this.renderTime = renderTime;
		this.renderScale = renderScale;
	}
}