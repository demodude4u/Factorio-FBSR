package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

import com.demod.fbsr.fp.FPSprite;

public class AccumulatorRendering extends SimpleSpriteRendering {
	@Override
	public FPSprite getSprite(LuaValue lua) {
		return new FPSprite(lua.get("chargable_graphics").get("picture"));
	}
}
