package com.demod.fbsr;

import java.awt.image.BufferedImage;

import com.google.common.collect.Multiset;

public class RenderResult {
	public final BufferedImage image;
	public final long renderTime;
	public final double renderScale;
	public final Multiset<String> unknownNames;

	public RenderResult(BufferedImage image, long renderTime, double renderScale, Multiset<String> unknownNames) {
		this.image = image;
		this.renderTime = renderTime;
		this.renderScale = renderScale;
		this.unknownNames = unknownNames;
	}
}