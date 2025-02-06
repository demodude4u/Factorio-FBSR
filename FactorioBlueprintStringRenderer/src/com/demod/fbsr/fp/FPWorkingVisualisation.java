package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Sprite;
import com.google.common.collect.ImmutableList;

public class FPWorkingVisualisation {
	public final boolean alwaysDraw;
	public final Optional<FPAnimation> animation;
	public final Optional<FPAnimation> northAnimation;
	public final Optional<FPAnimation> eastAnimation;
	public final Optional<FPAnimation> southAnimation;
	public final Optional<FPAnimation> westAnimation;

	public FPWorkingVisualisation(LuaValue lua) {
		alwaysDraw = lua.get("always_draw").optboolean(false);

		animation = FPUtils.opt(lua.get("animation"), FPAnimation::new);
		northAnimation = FPUtils.opt(lua.get("north_animation"), FPAnimation::new).or(() -> animation);
		eastAnimation = FPUtils.opt(lua.get("east_animation"), FPAnimation::new).or(() -> animation);
		southAnimation = FPUtils.opt(lua.get("south_animation"), FPAnimation::new).or(() -> animation);
		westAnimation = FPUtils.opt(lua.get("west_animation"), FPAnimation::new).or(() -> animation);
	}

	public void createSprites(Consumer<Sprite> consumer, Direction direction, int frame) {
		ImmutableList.of(northAnimation, eastAnimation, southAnimation, westAnimation).get(direction.cardinal())
				.ifPresent(animation -> {
					animation.createSprites(consumer, frame);
				});
	}

	public List<Sprite> createSprites(Direction direction, int frame) {
		List<Sprite> ret = new ArrayList<>();
		createSprites(ret::add, direction, frame);
		return ret;
	}
}
