package com.demod.fbsr;

import java.awt.image.BufferedImage;

public class RenderResult {
	public final BufferedImage image;
	public final long renderTime;

	public RenderResult(BufferedImage image, long renderTime) {
		this.image = image;
		this.renderTime = renderTime;
	}
}