package com.demod.fbsr;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.demod.fbsr.map.MapRect3D;

public abstract class Renderer {
	protected final MapRect3D bounds;
	protected final Layer layer;
	protected final boolean ignoreBoundsCalculation;

	public Renderer(Layer layer, Point2D.Double position, boolean ignoreBoundsCalculation) {
		this(layer, new MapRect3D(position.x, position.y, position.x, position.y, 0),
				ignoreBoundsCalculation);
	}

	public Renderer(Layer layer, MapRect3D bounds, boolean ignoreBoundsCalculation) {
		this.layer = layer;
		this.bounds = bounds;
		this.ignoreBoundsCalculation = ignoreBoundsCalculation;
	}

	public MapRect3D getBounds() {
		return bounds;
	}

	public Layer getLayer() {
		return layer;
	}

	public boolean ignoreBoundsCalculation() {
		return ignoreBoundsCalculation;
	}

	public abstract void render(Graphics2D g) throws Exception;
}
