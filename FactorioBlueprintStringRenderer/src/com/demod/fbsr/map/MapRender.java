package com.demod.fbsr.map;

import java.awt.Graphics2D;
import java.awt.Point;

import com.demod.fbsr.Layer;

public abstract class MapRender {
	protected final Layer layer;
	protected final MapPosition position;

	public MapRender(Layer layer, MapPosition position) {
		this.layer = layer;
		this.position = position;
	}

	public abstract void render(Graphics2D g);

	public Layer getLayer() {
		return layer;
	}

	public MapPosition getPosition() {
		return position;
	}
}
