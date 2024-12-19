package com.demod.fbsr.gui.part;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import com.demod.fbsr.gui.GUIBox;

public class GUIImage extends GUIPart {

	public final BufferedImage image;
	private final boolean scaled;

	public GUIImage(GUIBox box, BufferedImage image, boolean scaled) {
		super(box);
		this.image = image;
		this.scaled = scaled;
	}

	@Override
	public void render(Graphics2D g) {
		if (scaled) {
			AffineTransform xform = g.getTransform();
			int dstWidth = (int) (image.getWidth() / xform.getScaleX());
			int dstHeight = (int) (image.getHeight() / xform.getScaleY());
			int dx1 = box.x + box.width / 2 - dstWidth / 2;
			int dy1 = box.y + box.height / 2 - dstHeight / 2;
			int dx2 = dx1 + dstWidth;
			int dy2 = dy1 + dstHeight;
			g.drawImage(image, dx1, dy1, dx2, dy2, 0, 0, image.getWidth(), image.getHeight(), null);
		} else {
			g.drawImage(image, box.x + box.width / 2 - image.getWidth() / 2, //
					box.y + box.height / 2 - image.getHeight() / 2, null);
		}

	}
}
