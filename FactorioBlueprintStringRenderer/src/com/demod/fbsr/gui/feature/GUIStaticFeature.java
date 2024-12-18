package com.demod.fbsr.gui.feature;

import java.awt.Graphics2D;

import com.demod.fbsr.gui.GUIBox;

public class GUIStaticFeature extends GUISourcedFeature {
	public GUIStaticFeature(String filename, GUIBox source) {
		super(filename, source);
	}

	public void render(Graphics2D g, GUIBox rect) {
		g.drawImage(image, rect.x, rect.y, rect.x + rect.width, rect.y + rect.height, //
				source.x, source.y, source.x + source.width, source.y + source.height, null);
	}
}