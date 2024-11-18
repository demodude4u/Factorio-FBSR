package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Sprite;

public class FPAnimationVariations {

	public final Optional<FPAnimationSheet> sheet;
	public final Optional<List<FPAnimation>> animations;

	public FPAnimationVariations(LuaValue lua) {
		if (lua.istable()) {
			sheet = Optional.empty();
			animations = FPUtils.optList(lua, FPAnimation::new);
		} else {
			sheet = FPUtils.opt(lua.get("sheet"), FPAnimationSheet::new)
					.or(() -> Optional.of(new FPAnimationSheet(lua)));
			animations = Optional.empty();
		}
	}

	public void createSprites(Consumer<Sprite> consumer, int variation, int frame) {
		if (sheet.isPresent()) {
			sheet.get().createSprites(consumer, variation, frame);

		} else if (animations.isPresent()) {
			animations.get().get(variation).createSprites(consumer, frame);
		}
	}

	public List<Sprite> createSprites(int variation, int frame) {
		List<Sprite> ret = new ArrayList<>();
		createSprites(ret::add, variation, frame);
		return ret;
	}
}
