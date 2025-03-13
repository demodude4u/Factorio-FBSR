package com.demod.fbsr.map;

import java.awt.Color;
import java.util.Optional;

import com.demod.fbsr.Layer;
import com.demod.fbsr.LayeredSpriteDef;
import com.demod.fbsr.SpriteDef;

public class MapTintOverrideSprite extends MapSprite {

	private final Color tint;

	public MapTintOverrideSprite(LayeredSpriteDef def, MapPosition pos, Color tint) {
		super(def, pos);
		this.tint = tint;
	}

	public MapTintOverrideSprite(SpriteDef def, Layer layer, MapPosition pos, Color tint) {
		super(def, layer, pos);
		this.tint = tint;
	}

	@Override
	protected Optional<Color> tintOverride(Optional<Color> tint) {
		return Optional.of(this.tint);
	}
}
