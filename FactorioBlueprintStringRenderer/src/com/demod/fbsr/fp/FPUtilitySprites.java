package com.demod.fbsr.fp;

import com.demod.factorio.fakelua.LuaValue;

public class FPUtilitySprites {
	public final FPSprite clock;
	public final FPSprite filterBlacklist;

	public FPUtilitySprites(LuaValue lua) {
		clock = new FPSprite(lua.get("clock"), false);
		filterBlacklist = new FPSprite(lua.get("filter_blacklist"), false);
	}
}
