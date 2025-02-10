package com.demod.fbsr.fp;

import java.util.List;

import com.demod.factorio.FactorioData;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.SpriteWithLayer;

public class FPLayeredSpriteVariations {
	private final List<FPLayeredSprite> layeredSprites;

	public FPLayeredSpriteVariations(LuaValue lua) {
		layeredSprites = FPUtils.list(lua, FPLayeredSprite::new);
	}

	public List<SpriteWithLayer> createSpritesWithLayers(FactorioData data, int variation) {
		return layeredSprites.get(variation).createSpritesWithLayers(data);
	}

	public int getVariationCount() {
		return layeredSprites.size();
	}

}
