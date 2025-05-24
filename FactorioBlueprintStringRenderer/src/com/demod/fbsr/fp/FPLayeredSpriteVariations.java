package com.demod.fbsr.fp;

import java.util.List;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.ModsProfile;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.LayeredSpriteDef;

public class FPLayeredSpriteVariations {
	private final List<FPLayeredSprite> layeredSprites;

	public FPLayeredSpriteVariations(ModsProfile profile, LuaValue lua) {
		layeredSprites = FPUtils.list(profile, lua, FPLayeredSprite::new);
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
