package com.demod.fbsr.gui.part;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import com.demod.fbsr.AtlasManager.AtlasRef;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.gui.GUIBox;

public class GUIImageDef extends GUIPart {

	public final ImageDef def;

	public GUIImageDef(GUIBox box, ImageDef def) {
		super(box);
		this.def = def;
	}

	@Override
	public void render(Graphics2D g) {
		AtlasRef ref = def.getAtlasRef();
		BufferedImage imageSheet = ref.getAtlas().getBufferedImage();
		Rectangle rect = ref.getRect();

		g.drawImage(imageSheet, //
				box.x, //
				box.y, //
				box.x + box.width, //
				box.y + box.height, //
				rect.x, //
				rect.y, //
				rect.x + rect.width, //
				rect.y + rect.height, //
				null);
	}
}
