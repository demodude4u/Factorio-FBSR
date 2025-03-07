package com.demod.fbsr.gui.part;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import com.demod.fbsr.AtlasManager.AtlasRef;
import com.demod.fbsr.ImageDef;
import com.demod.fbsr.gui.GUIBox;

public class GUIImageDef extends GUIPart {

	public final ImageDef def;
	private final double scale;
	private final boolean preScaled;

	public GUIImageDef(GUIBox box, ImageDef def, boolean preScaled) {
		this(box, def, 1.0, preScaled);
	}

	public GUIImageDef(GUIBox box, ImageDef def, double scale, boolean preScaled) {
		super(box);
		this.def = def;
		this.scale = scale;
		this.preScaled = preScaled;
	}

	@Override
	public void render(Graphics2D g) {
		AtlasRef ref = def.getAtlasRef();
		BufferedImage imageSheet = ref.getAtlas().getBufferedImage();
		Rectangle rect = ref.getRect();

		if (preScaled) {
			AffineTransform xform = g.getTransform();
			int dstWidth = (int) (rect.width / xform.getScaleX());
			int dstHeight = (int) (rect.height / xform.getScaleY());
			int dx1 = (int) (box.x + box.width / 2 - scale * dstWidth / 2);
			int dy1 = (int) (box.y + box.height / 2 - scale * dstHeight / 2);
			int dx2 = (int) (dx1 + scale * dstWidth);
			int dy2 = (int) (dy1 + scale * dstHeight);
			g.drawImage(imageSheet, dx1, dy1, dx2, dy2, rect.x, rect.y, rect.x + rect.width, rect.y + rect.height,
					null);
		} else {
			int dx1 = (int) (box.x + box.width / 2 - scale * rect.width / 2);
			int dy1 = (int) (box.y + box.height / 2 - scale * rect.height / 2);
			int dx2 = (int) (dx1 + scale * rect.width);
			int dy2 = (int) (dy1 + scale * rect.height);
			g.drawImage(imageSheet, dx1, dy1, dx2, dy2, rect.x, rect.y, rect.x + rect.width, rect.y + rect.height,
					null);
		}

	}
}
