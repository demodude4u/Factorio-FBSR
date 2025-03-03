package com.demod.fbsr.fp;

import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.ImageDef;

public class FPTileTransitionVariantLayout {
	public final Optional<FPTileSpriteLayoutVariant> side;
	public final Optional<FPTileSpriteLayoutVariant> doubleSide;
	public final Optional<FPTileSpriteLayoutVariant> outerCorner;
	public final Optional<FPTileSpriteLayoutVariant> uTransition;
	public final Optional<FPTileSpriteLayoutVariant> oTransition;
	public final Optional<FPTileSpriteLayoutVariant> innerCorner;

	public FPTileTransitionVariantLayout(LuaValue lua) {
		side = FPUtils.opt(lua.get("side"), FPTileSpriteLayoutVariant::new);
		doubleSide = FPUtils.opt(lua.get("double_side"), FPTileSpriteLayoutVariant::new);
		outerCorner = FPUtils.opt(lua.get("outer_corner"), FPTileSpriteLayoutVariant::new);
		uTransition = FPUtils.opt(lua.get("u_transition"), FPTileSpriteLayoutVariant::new);
		oTransition = FPUtils.opt(lua.get("o_transition"), FPTileSpriteLayoutVariant::new);
		innerCorner = FPUtils.opt(lua.get("inner_corner"), FPTileSpriteLayoutVariant::new);
	}

	public void getDefs(Consumer<ImageDef> consumer) {
		side.ifPresent(fp -> fp.getDefs().forEach(consumer));
		doubleSide.ifPresent(fp -> fp.getDefs().forEach(consumer));
		outerCorner.ifPresent(fp -> fp.getDefs().forEach(consumer));
		uTransition.ifPresent(fp -> fp.getDefs().forEach(consumer));
		oTransition.ifPresent(fp -> fp.getDefs().forEach(consumer));
		innerCorner.ifPresent(fp -> fp.getDefs().forEach(consumer));
	}
}
