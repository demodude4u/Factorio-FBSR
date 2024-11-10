package com.demod.fbsr;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.awt.image.BufferedImage;

public class SpriteDef {
	private final BufferedImage image;
	private final Rectangle source;
	private final Rectangle2D.Double bounds;
	private final boolean shadow;
	private final int order;

	public SpriteDef(BufferedImage image, Rectangle source, Double bounds) {
		this.image = image;
		this.source = source;
		this.bounds = bounds;
		this.shadow = false;
		this.order = 0;
	}

	public SpriteDef(BufferedImage image, Rectangle source, Double bounds, boolean shadow, int order) {
		this.image = image;
		this.source = source;
		this.bounds = bounds;
		this.shadow = shadow;
		this.order = order;
	}

	public SpriteDef(Sprite sprite) {
		this.image = sprite.image;
		this.source = new Rectangle(sprite.source);
		this.bounds = new Rectangle2D.Double(sprite.bounds.x, sprite.bounds.y, sprite.bounds.width,
				sprite.bounds.height);
		this.shadow = sprite.shadow;
		this.order = sprite.order;
	}

	public SpriteDef(SpriteDef sprite) {
		this.image = sprite.image;
		this.source = new Rectangle(sprite.source);
		this.bounds = new Rectangle2D.Double(sprite.bounds.x, sprite.bounds.y, sprite.bounds.width,
				sprite.bounds.height);
		this.shadow = sprite.shadow;
		this.order = sprite.order;
	}

	public Sprite createSprite() {
		Sprite sprite = new Sprite();
		sprite.image = image;
		sprite.source = new Rectangle(source);
		sprite.bounds = new Rectangle2D.Double(bounds.x, bounds.y, bounds.width, bounds.height);
		sprite.shadow = shadow;
		sprite.order = order;
		return sprite;
	}

	public Rectangle2D.Double getBounds() {
		return bounds;
	}

	public BufferedImage getImage() {
		return image;
	}

	public int getOrder() {
		return order;
	}

	public Rectangle getSource() {
		return source;
	}

	public boolean isShadow() {
		return shadow;
	}

	public SpriteDef withImage(BufferedImage image) {
		return new SpriteDef(image, source, bounds, shadow, order);
	}

	public SpriteDef withOrder(int order) {
		return new SpriteDef(image, source, bounds, shadow, order);
	}
}