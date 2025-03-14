package com.demod.fbsr.fp;

import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.def.ImageDef;

public class FPTileTransitionVariantLayout {
	public final Optional<FPTileSpriteLayoutVariant> side;
	public final Optional<FPTileSpriteLayoutVariant> doubleSide;
	public final Optional<FPTileSpriteLayoutVariant> outerCorner;
	public final Optional<FPTileSpriteLayoutVariant> uTransition;
	public final Optional<FPTileSpriteLayoutVariant> oTransition;
	public final Optional<FPTileSpriteLayoutVariant> innerCorner;

	public FPTileTransitionVariantLayout(LuaValue lua) {
		side = FPUtils.opt(lua.get("side"), l -> new FPTileSpriteLayoutVariant(l, 4));
		doubleSide = FPUtils.opt(lua.get("double_side"), l -> new FPTileSpriteLayoutVariant(l, 2));
		outerCorner = FPUtils.opt(lua.get("outer_corner"), l -> new FPTileSpriteLayoutVariant(l, 4));
		uTransition = FPUtils.opt(lua.get("u_transition"), l -> new FPTileSpriteLayoutVariant(l, 4));
		oTransition = FPUtils.opt(lua.get("o_transition"), l -> new FPTileSpriteLayoutVariant(l, 1));
		innerCorner = FPUtils.opt(lua.get("inner_corner"), l -> new FPTileSpriteLayoutVariant(l, 4));
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
