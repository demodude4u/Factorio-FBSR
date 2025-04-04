package com.demod.fbsr.gui.feature;

import java.awt.Graphics2D;

import com.demod.fbsr.gui.GUIBox;

public class GUIStaticFeature extends GUISourcedFeature {
	private final int width;
	private final int height;

	public GUIStaticFeature(String filename, GUIBox source) {
		super(filename, source);
		width = source.width;
		height = source.height;
	}

	public void render(Graphics2D g, GUIBox rect) {
		drawImage(g, rect.x, rect.y, rect.x + rect.width, rect.y + rect.height, 0, 0, width, height);
	}
}