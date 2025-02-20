package com.demod.fbsr.map;

import com.demod.fbsr.TileRendererFactory;
import com.demod.fbsr.bs.BSTile;

public class MapTile {
	private final BSTile tile;
	private final TileRendererFactory factory;

	public MapTile(BSTile tile, TileRendererFactory factory) {
		this.tile = tile;
		this.factory = factory;
	}

	public BSTile getTile() {
		return tile;
	}

	public TileRendererFactory getFactory() {
		return factory;
	}
}
