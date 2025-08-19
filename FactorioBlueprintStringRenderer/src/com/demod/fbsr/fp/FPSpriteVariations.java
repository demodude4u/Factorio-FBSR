package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Profile;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.SpriteDef;

public class FPSpriteVariations {

	public final Optional<FPSpriteSheet> sheet;
	public final Optional<List<FPSprite>> sprites;

	public FPSpriteVariations(Profile profile, LuaValue lua) {
		if (lua.isarray()) { // This is a Sprite array
			sheet = Optional.empty();
			sprites = FPUtils.optList(profile, lua, FPSprite::new);

		} else if (!lua.get("sheet").isnil()) {// This is a SpriteVariations
			sheet = Optional.of(new FPSpriteSheet(profile, lua.get("sheet")));
			sprites = Optional.empty();
		
		} else {// This is a SpriteSheet
			sheet = Optional.of(new FPSpriteSheet(profile, lua));
			sprites = Optional.empty();
		}
	}

	public void defineSprites(Consumer<? super SpriteDef> consumer, int variation) {
		if (sheet.isPresent()) {
			sheet.get().defineSprites(consumer, variation);

		} else if (sprites.isPresent()) {
			sprites.get().get(variation).defineSprites(consumer);
		}
	}

	public List<SpriteDef> defineSprites(int variation) {
		List<SpriteDef> ret = new ArrayList<>();
		defineSprites(ret::add, variation);
		return ret;
	}

	public void getDefs(Consumer<ImageDef> register) {
		int variationCount = getVariationCount();
		for (int variation = 0; variation < variationCount; variation++) {
			defineSprites(register, variation);
		}
	}

	public int getVariationCount() {
		if (sheet.isPresent()) {
			return sheet.get().getFrameCount();

		} else if (sprites.isPresent()) {
			return sprites.get().size();
		}

		return 0;
	}
}
