package com.demod.fbsr.map;

import com.demod.fbsr.ModdingResolver;
import com.demod.fbsr.TileRendererFactory;
import com.demod.fbsr.bs.BSTile;

public class MapTile {
	private final BSTile tile;
	private final TileRendererFactory factory;
	private final ModdingResolver resolver;

	private final MapPosition position;

	public MapTile(BSTile tile, TileRendererFactory factory, ModdingResolver resolver) {
		this.tile = tile;
		this.factory = factory;
		this.resolver = resolver;

		position = tile.position.createPoint();
	}

	public BSTile fromBlueprint() {
		return tile;
	}

	public TileRendererFactory getFactory() {
		return factory;
	}

	public MapPosition getPosition() {
		return position;
	}

	public ModdingResolver getResolver() {
		return resolver;
	}
}
