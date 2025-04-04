package com.demod.fbsr.map;

import java.awt.Graphics2D;

import com.demod.fbsr.Layer;

public abstract class MapRenderable {
	protected final Layer layer;

	public MapRenderable(Layer layer) {
		this.layer = layer;
	}

	public Layer getLayer() {
		return layer;
	}

	public abstract void render(Graphics2D g);
}
