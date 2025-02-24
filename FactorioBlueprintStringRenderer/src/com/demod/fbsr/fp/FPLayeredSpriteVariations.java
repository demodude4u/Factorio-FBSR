package com.demod.fbsr.fp;

import java.util.List;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.LayeredSpriteDef;

public class FPLayeredSpriteVariations {
	private final List<FPLayeredSprite> layeredSprites;

	public FPLayeredSpriteVariations(LuaValue lua) {
		layeredSprites = FPUtils.list(lua, FPLayeredSprite::new);
	}

	public List<LayeredSpriteDef> defineLayeredSprites(int variation) {
		return layeredSprites.get(variation).defineLayeredSprites();
	}

	public int getVariationCount() {
		return layeredSprites.size();
	}

}
