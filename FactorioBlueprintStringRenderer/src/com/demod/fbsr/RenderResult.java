package com.demod.fbsr;

import java.awt.image.BufferedImage;

import com.google.common.collect.Multiset;

public class RenderResult {
	public final RenderRequest request;
	public final BufferedImage image;
	public final long renderTime;
	public final double renderScale;
	public final Multiset<String> unknownNames;

	public RenderResult(RenderRequest request, BufferedImage image, long renderTime, double renderScale,
			Multiset<String> unknownNames) {
		this.request = request;
		this.image = image;
		this.renderTime = renderTime;
		this.renderScale = renderScale;
		this.unknownNames = unknownNames;
	}
}