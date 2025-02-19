package com.demod.fbsr;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.awt.image.BufferedImage;

import com.demod.fbsr.map.MapRect3D;

public class Sprite {
	private Layer layer;
	private BufferedImage image;
	private Rectangle source;
	private Rectangle2D.Double bounds;

	public Sprite(Layer layer, BufferedImage image, Rectangle source, Rectangle.Double bounds) {
		this.layer = layer;
		this.image = image;
		this.source = source;
		this.bounds = bounds;
	}

	public Rectangle2D.Double getBounds() {
		return bounds;
	}

	public BufferedImage getImage() {
		return image;
	}

	public Layer getLayer() {
		return layer;
	}

	public Rectangle getSource() {
		return source;
	}

	public void setBounds(Rectangle2D.Double bounds) {
		this.bounds = bounds;
	}

	public void setImage(BufferedImage image) {
		this.image = image;
	}

	public void setLayer(Layer layer) {
		this.layer = layer;
	}

	public void setSource(Rectangle source) {
		this.source = source;
	}

	public Renderer createRenderer() {
		return new Renderer(layer, new MapRect3D(bounds, 0)) {

			@Override
			public void render(Graphics2D g) throws Exception {
				// TODO Auto-generated method stub

			}
		};
	}
}