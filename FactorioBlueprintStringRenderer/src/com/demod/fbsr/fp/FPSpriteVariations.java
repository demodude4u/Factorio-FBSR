package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Sprite;

public class FPSpriteVariations {

	public final Optional<FPSpriteSheet> sheet;
	public final Optional<List<FPSprite>> sprites;

	public FPSpriteVariations(LuaValue lua) {
		if (lua.istable()) {
			sheet = Optional.empty();
			sprites = FPUtils.optList(lua, FPSprite::new);
		} else {
			sheet = FPUtils.opt(lua.get("sheet"), FPSpriteSheet::new).or(() -> Optional.of(new FPSpriteSheet(lua)));
			sprites = Optional.empty();
		}
	}

	public void createSprites(Consumer<Sprite> consumer, int variation) {
		if (sheet.isPresent()) {
			sheet.get().createSprites(consumer, variation);

		} else if (sprites.isPresent()) {
			sprites.get().get(variation).createSprites(consumer);
		}
	}

	public List<Sprite> createSprites(int variation) {
		List<Sprite> ret = new ArrayList<>();
		createSprites(ret::add, variation);
		return ret;
	}

	public int getVariationCount() {
		if (sheet.isPresent()) {
			return sheet.get().variationCount;

		} else if (sprites.isPresent()) {
			return sprites.get().size();
		}

		return 0;
	}
}
