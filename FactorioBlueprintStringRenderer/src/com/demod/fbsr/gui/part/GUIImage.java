package com.demod.fbsr.gui.part;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import com.demod.fbsr.gui.GUIBox;

public class GUIImage extends GUIPart {

	public BufferedImage image;
	private double scale;
	private boolean preScaled;

	public GUIImage(GUIBox box, BufferedImage image, boolean preScaled) {
		this(box, image, 1.0, preScaled);
	}

	public GUIImage(GUIBox box, BufferedImage image, double scale, boolean preScaled) {
		super(box);
		this.image = image;
		this.scale = scale;
		this.preScaled = preScaled;
	}

	@Override
	public void render(Graphics2D g) {
		if (preScaled) {
			AffineTransform xform = g.getTransform();
			int dstWidth = (int) (image.getWidth() / xform.getScaleX());
			int dstHeight = (int) (image.getHeight() / xform.getScaleY());
			int dx1 = (int) (box.x + box.width / 2 - scale * dstWidth / 2);
			int dy1 = (int) (box.y + box.height / 2 - scale * dstHeight / 2);
			int dx2 = (int) (dx1 + scale * dstWidth);
			int dy2 = (int) (dy1 + scale * dstHeight);
			g.drawImage(image, dx1, dy1, dx2, dy2, 0, 0, image.getWidth(), image.getHeight(), null);
		} else {
			int x = (int) (box.x + box.width / 2 - scale * image.getWidth() / 2);
			int y = (int) (box.y + box.height / 2 - scale * image.getHeight() / 2);
			int w = (int) (scale * image.getWidth());
			int h = (int) (scale * image.getHeight());
			g.drawImage(image, x, y, w, h, null);
		}

	}
}
