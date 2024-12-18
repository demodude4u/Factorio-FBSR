package com.demod.fbsr.gui.part;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.demod.fbsr.gui.GUIBox;

public class GUIImage extends GUIPart {

	public final BufferedImage image;

	public GUIImage(GUIBox box, BufferedImage image) {
		super(box);
		this.image = image;
	}

	@Override
	public void render(Graphics2D g) {
		g.drawImage(image, box.x + box.width / 2 - image.getWidth() / 2, //
				box.y + box.height / 2 - image.getHeight() / 2, null);
	}
}
