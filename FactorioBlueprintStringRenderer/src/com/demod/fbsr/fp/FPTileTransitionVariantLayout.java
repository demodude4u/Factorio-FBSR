package com.demod.fbsr.fp;

import java.util.Optional;

import org.luaj.vm2.LuaValue;

import com.demod.fbsr.FPUtils;

public class FPTileTransitionVariantLayout {
	public final Optional<FPTileSpriteLayoutVariant> side;
	public final Optional<FPTileSpriteLayoutVariant> outerCorner;
	public final Optional<FPTileSpriteLayoutVariant> uTransition;
	public final Optional<FPTileSpriteLayoutVariant> oTransition;
	public final Optional<FPTileSpriteLayoutVariant> innerCorner;

	public FPTileTransitionVariantLayout(LuaValue lua) {
		side = FPUtils.opt(lua.get("side"), FPTileSpriteLayoutVariant::new);
		outerCorner = FPUtils.opt(lua.get("outer_corner"), FPTileSpriteLayoutVariant::new);
		uTransition = FPUtils.opt(lua.get("u_transition"), FPTileSpriteLayoutVariant::new);
		oTransition = FPUtils.opt(lua.get("o_transition"), FPTileSpriteLayoutVariant::new);
		innerCorner = FPUtils.opt(lua.get("inner_corner"), FPTileSpriteLayoutVariant::new);
	}
}
