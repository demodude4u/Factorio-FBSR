package com.demod.fbsr.gui.feature;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import com.demod.fbsr.AtlasManager;
import com.demod.fbsr.AtlasManager.AtlasRef;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.gui.GUIBox;

public abstract class GUISourcedFeature {
	public final ImageDef def;

	public GUISourcedFeature(String filename, GUIBox source) {
		def = new ImageDef(filename, new Rectangle(source.x, source.y, source.width, source.height));
		def.setTrimmable(false);
		AtlasManager.registerDef(def);
	}

	protected void drawImage(Graphics2D g, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2) {
		AtlasRef ref = def.getAtlasRef();
		BufferedImage buf = ref.getAtlas().getBufferedImage();
		Rectangle rect = ref.getRect();
		g.drawImage(buf, dx1, dy1, dx2, dy2, rect.x + sx1, rect.y + sy1, rect.x + sx2, rect.y + sy2, null);
	}
}