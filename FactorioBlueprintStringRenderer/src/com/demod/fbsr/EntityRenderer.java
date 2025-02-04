package com.demod.fbsr;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public abstract class EntityRenderer extends Renderer {

	public EntityRenderer(Layer layer, Point2D.Double position, boolean ignoreBoundsCalculation) {
		super(layer, new Rectangle2D.Double(position.x, position.y, 0, 0), ignoreBoundsCalculation);
	}

	public EntityRenderer(Layer layer, Rectangle2D.Double bounds, boolean ignoreBoundsCalculation) {
		super(layer, bounds, ignoreBoundsCalculation);
	}

	public abstract void renderShadows(Graphics2D g) throws Exception;
}
