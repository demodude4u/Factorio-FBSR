package com.demod.fbsr.fp;

import java.util.List;

import org.luaj.vm2.LuaValue;

import com.demod.fbsr.FPUtils;
import com.demod.fbsr.SpriteWithLayer;

public class FPLayeredSpriteVariations {
	private final List<FPLayeredSprite> layeredSprites;

	public FPLayeredSpriteVariations(LuaValue lua) {
		layeredSprites = FPUtils.list(lua, FPLayeredSprite::new);
	}

	public List<SpriteWithLayer> createSpritesWithLayers(int variation) {
		return layeredSprites.get(variation).createSpritesWithLayers();
	}

}
