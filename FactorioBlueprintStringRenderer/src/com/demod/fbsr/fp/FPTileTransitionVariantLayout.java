package com.demod.fbsr.fp;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Profile;
import com.demod.fbsr.def.ImageDef;

public class FPTileTransitionVariantLayout {
	public final Optional<FPTileSpriteLayoutVariant> side;
	public final Optional<FPTileSpriteLayoutVariant> doubleSide;
	public final Optional<FPTileSpriteLayoutVariant> outerCorner;
	public final Optional<FPTileSpriteLayoutVariant> uTransition;
	public final Optional<FPTileSpriteLayoutVariant> oTransition;
	public final Optional<FPTileSpriteLayoutVariant> innerCorner;

	public final FPTileSpriteLayoutDefaults defaults;
	public final FPTileSpriteLayoutDefaults sideDefaults;
	public final FPTileSpriteLayoutDefaults doubleSideDefaults;
	public final FPTileSpriteLayoutDefaults outerCornerDefaults;
	public final FPTileSpriteLayoutDefaults uTransitionDefaults;
	public final FPTileSpriteLayoutDefaults oTransitionDefaults;
	public final FPTileSpriteLayoutDefaults innerCornerDefaults;

	public FPTileTransitionVariantLayout(Profile profile, LuaValue lua, Optional<String> overrideSpritesheet,
			FPTileSpriteLayoutDefaults parentDefaults) {
		defaults = new FPTileSpriteLayoutDefaults(lua, "", false);
		sideDefaults = new FPTileSpriteLayoutDefaults(lua, "side_", true);
		doubleSideDefaults = new FPTileSpriteLayoutDefaults(lua, "double_side_", true);
		outerCornerDefaults = new FPTileSpriteLayoutDefaults(lua, "outer_corner_", true);
		uTransitionDefaults = new FPTileSpriteLayoutDefaults(lua, "u_transition_", true);
		oTransitionDefaults = new FPTileSpriteLayoutDefaults(lua, "o_transition_", true);
		innerCornerDefaults = new FPTileSpriteLayoutDefaults(lua, "inner_corner_", true);

		side = FPUtils.opt(profile, lua.get("side"), (p, l) -> new FPTileSpriteLayoutVariant(p, l, overrideSpritesheet,
				sideDefaults.or(defaults), OptionalInt.of(4)));
		doubleSide = FPUtils.opt(profile, lua.get("double_side"), (p, l) -> new FPTileSpriteLayoutVariant(p, l, overrideSpritesheet,
				doubleSideDefaults.or(defaults).or(parentDefaults), OptionalInt.of(2)));
		outerCorner = FPUtils.opt(profile, lua.get("outer_corner"), (p, l) -> new FPTileSpriteLayoutVariant(p, l, overrideSpritesheet,
				outerCornerDefaults.or(defaults).or(parentDefaults), OptionalInt.of(4)));
		uTransition = FPUtils.opt(profile, lua.get("u_transition"), (p, l) -> new FPTileSpriteLayoutVariant(p, l, overrideSpritesheet,
				uTransitionDefaults.or(defaults).or(parentDefaults), OptionalInt.of(4)));
		oTransition = FPUtils.opt(profile, lua.get("o_transition"), (p, l) -> new FPTileSpriteLayoutVariant(p, l, overrideSpritesheet,
				oTransitionDefaults.or(defaults).or(parentDefaults), OptionalInt.of(1)));
		innerCorner = FPUtils.opt(profile, lua.get("inner_corner"), (p, l) -> new FPTileSpriteLayoutVariant(p, l, overrideSpritesheet,
				innerCornerDefaults.or(defaults).or(parentDefaults), OptionalInt.of(4)));
	}

	public void getDefs(Consumer<ImageDef> register) {
		side.ifPresent(fp -> fp.getDefs(register));
		doubleSide.ifPresent(fp -> fp.getDefs(register));
		outerCorner.ifPresent(fp -> fp.getDefs(register));
		uTransition.ifPresent(fp -> fp.getDefs(register));
		oTransition.ifPresent(fp -> fp.getDefs(register));
		innerCorner.ifPresent(fp -> fp.getDefs(register));
	}
}
