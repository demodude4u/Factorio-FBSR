package com.demod.fbsr;

import java.util.List;

public class SpritesWithLayer {

	private final Layer layer;
	private final List<Sprite> sprites;

	public SpritesWithLayer(Layer layer, List<Sprite> sprites) {
		this.layer = layer;
		this.sprites = sprites;
	}

	public Layer getLayer() {
		return layer;
	}

	public List<Sprite> getSprites() {
		return sprites;
	}

}
