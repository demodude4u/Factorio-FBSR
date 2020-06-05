package com.demod.fbsr;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class Sprite {
	public BufferedImage image;
	public Rectangle source;
	public Rectangle2D.Double bounds;
	public boolean shadow = false;
	public int order = 0;

	public Sprite() {
	}

	public Sprite(Sprite other) {
		image = other.image;
		source = new Rectangle(other.source);
		bounds = new Rectangle2D.Double(other.bounds.x, other.bounds.y, other.bounds.width, other.bounds.height);
		shadow = other.shadow;
	}
}