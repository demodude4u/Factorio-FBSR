package com.demod.fbsr.fp;

import org.luaj.vm2.LuaValue;

public class FPTileTransitionVariantLayout {
	public final FPTileSpriteLayoutVariant side;
	public final FPTileSpriteLayoutVariant outerCorner;
	public final FPTileSpriteLayoutVariant uTransition;
	public final FPTileSpriteLayoutVariant oTransition;
	public final FPTileSpriteLayoutVariant innerCorner;

	public FPTileTransitionVariantLayout(LuaValue lua) {
		side = new FPTileSpriteLayoutVariant(lua.get("side"));
		outerCorner = new FPTileSpriteLayoutVariant(lua.get("outer_corner"));
		uTransition = new FPTileSpriteLayoutVariant(lua.get("u_transition"));
		oTransition = new FPTileSpriteLayoutVariant(lua.get("o_transition"));
		innerCorner = new FPTileSpriteLayoutVariant(lua.get("inner_corner"));
	}
}
