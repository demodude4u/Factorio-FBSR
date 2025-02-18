package com.demod.fbsr;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public abstract class Renderer {
	protected final BoundingBoxWithHeight bounds;
	protected final Layer layer;
	protected final boolean ignoreBoundsCalculation;

	public Renderer(Layer layer, Point2D.Double position, boolean ignoreBoundsCalculation) {
		this(layer, new BoundingBoxWithHeight(position.x, position.y, position.x, position.y, 0),
				ignoreBoundsCalculation);
	}

	public Renderer(Layer layer, BoundingBoxWithHeight bounds, boolean ignoreBoundsCalculation) {
		this.layer = layer;
		this.bounds = bounds;
		this.ignoreBoundsCalculation = ignoreBoundsCalculation;
	}

	public BoundingBoxWithHeight getBounds() {
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
