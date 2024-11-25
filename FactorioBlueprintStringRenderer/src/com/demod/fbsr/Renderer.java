package com.demod.fbsr;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public abstract class Renderer {
	// TODO switch over to the actual layer names and use them from the lua
	public static enum Layer {
		TILE1, TILE2, TILE3, //
		RAIL_STONE_BACKGROUND, RAIL_STONE, LOGISTICS_RAIL_IO, RAIL_TIES, RAIL_BACKPLATES, RAIL_METALS, //
		SHADOW_BUFFER, //
		ENTITY, LOGISTICS_MOVE, ENTITY2, ENTITY3, //
		OVERLAY, OVERLAY2, OVERLAY3, OVERLAY4, //
		LOGISTICS_WARP, //
		WIRE, //
		ELEVATED_RAIL_STONE_BACKGROUND, ELEVATED_RAIL_STONE, ELEVATED_LOGISTICS_RAIL_IO, ELEVATED_RAIL_TIES,
		ELEVATED_RAIL_BACKPLATES, ELEVATED_RAIL_METALS, //
		DEBUG, DEBUG_RA1, DEBUG_RA2, DEBUG_LA1, DEBUG_LA2, DEBUG_P //
		;
	}

	protected final Rectangle2D.Double bounds;
	protected final Layer layer;

	public Renderer(Layer layer, Point2D.Double position) {
		this(layer, new Rectangle2D.Double(position.x, position.y, 0, 0));
	}

	public Renderer(Layer layer, Rectangle2D.Double bounds) {
		this.layer = layer;
		this.bounds = bounds;
	}

	public Rectangle2D.Double getBounds() {
		return bounds;
	}

	public Layer getLayer() {
		return layer;
	}

	public abstract void render(Graphics2D g) throws Exception;
}
