package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Profile;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.SpriteDef;
import com.google.common.collect.ImmutableList;

public class FPWorkingVisualisation {
	public final Optional<String> name;
	public final boolean alwaysDraw;
	public final Optional<FPAnimation> animation;
	public final Optional<FPAnimation> northAnimation;
	public final Optional<FPAnimation> eastAnimation;
	public final Optional<FPAnimation> southAnimation;
	public final Optional<FPAnimation> westAnimation;

	public FPWorkingVisualisation(Profile profile, LuaValue lua) {
		name = FPUtils.optString(lua.get("name"));
		alwaysDraw = lua.get("always_draw").optboolean(false);

		animation = FPUtils.opt(profile, lua.get("animation"), FPAnimation::new);
		northAnimation = FPUtils.opt(profile, lua.get("north_animation"), FPAnimation::new).or(() -> animation);
		eastAnimation = FPUtils.opt(profile, lua.get("east_animation"), FPAnimation::new).or(() -> animation);
		southAnimation = FPUtils.opt(profile, lua.get("south_animation"), FPAnimation::new).or(() -> animation);
		westAnimation = FPUtils.opt(profile, lua.get("west_animation"), FPAnimation::new).or(() -> animation);
	}

	public void defineSprites(Consumer<? super SpriteDef> consumer, Direction direction, int frame) {
		ImmutableList.of(northAnimation, eastAnimation, southAnimation, westAnimation).get(direction.cardinal())
				.ifPresent(animation -> {
					animation.defineSprites(consumer, frame);
				});
	}

	public void getDefs(Consumer<ImageDef> register, int frame) {
		northAnimation.ifPresent(a -> a.defineSprites(register, frame));
		eastAnimation.ifPresent(a -> a.defineSprites(register, frame));
		southAnimation.ifPresent(a -> a.defineSprites(register, frame));
		westAnimation.ifPresent(a -> a.defineSprites(register, frame));
	}

	public List<SpriteDef> defineSprites(Direction direction, int frame) {
		List<SpriteDef> ret = new ArrayList<>();
		defineSprites(ret::add, direction, frame);
		return ret;
	}
}
