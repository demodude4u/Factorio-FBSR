package com.demod.fbsr.gui.feature;

import java.awt.image.BufferedImage;

import com.demod.fbsr.FactorioManager;
import com.demod.fbsr.gui.GUIBox;

public abstract class GUISourcedFeature {
	public final String filename;
	public final GUIBox source;
	public final BufferedImage image;

	public GUISourcedFeature(String filename, GUIBox source) {
		this.filename = filename;
		this.source = source;
		image = FactorioManager.getBaseData().getModImage(filename);
	}
}