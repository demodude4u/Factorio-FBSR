package com.demod.fbsr;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;

public class SpriteWithLayer {
	public static List<SpritesWithLayer> groupByLayer(List<SpriteWithLayer> sprites) {
		ArrayListMultimap<Layer, Sprite> map = ArrayListMultimap.create();
		for (SpriteWithLayer spriteWithLayer : sprites) {
			map.put(spriteWithLayer.getLayer(), spriteWithLayer.getSprite());
		}
		List<SpritesWithLayer> ret = new ArrayList<>();
		for (Layer layer : map.keySet()) {
			ret.add(new SpritesWithLayer(layer, map.get(layer)));
		}
		return ret;
	}

	private final Layer layer;

	private final Sprite sprite;

	public SpriteWithLayer(Layer layer, Sprite sprite) {
		this.layer = layer;
		this.sprite = sprite;
	}

	public Layer getLayer() {
		return layer;
	}

	public Sprite getSprite() {
		return sprite;
	}
}