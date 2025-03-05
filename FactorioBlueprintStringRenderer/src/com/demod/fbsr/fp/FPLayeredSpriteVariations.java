package com.demod.fbsr.fp;

import java.util.List;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.ImageDef;
import com.demod.fbsr.LayeredSpriteDef;

public class FPLayeredSpriteVariations {
	private final List<FPLayeredSprite> layeredSprites;

	public FPLayeredSpriteVariations(LuaValue lua) {
		layeredSprites = FPUtils.list(lua, FPLayeredSprite::new);
	}

	public void defineLayeredSprites(Consumer<? super LayeredSpriteDef> consumer, int variation) {
		layeredSprites.get(variation).defineLayeredSprites(consumer);
	}

	public void getDefs(Consumer<ImageDef> register) {
		layeredSprites.forEach(fp -> fp.defineLayeredSprites(register));
	}

	public int getVariationCount() {
		return layeredSprites.size();
	}
}
