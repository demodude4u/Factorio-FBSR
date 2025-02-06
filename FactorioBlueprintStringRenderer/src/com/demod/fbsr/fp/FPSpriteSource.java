package com.demod.fbsr.fp;

import java.util.Optional;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;

public class FPSpriteSource {

	public final Optional<String> filename;
	public final int width;
	public final int height;
	public final int x;
	public final int y;

	public FPSpriteSource(LuaValue lua) {
		// XXX sometimes optional, sometimes required, depends on override
		filename = FPUtils.optString(lua.get("filename"));

		LuaValue luaSize = lua.get("size");
		if (luaSize.isnil()) {
			width = lua.get("width").optint(0);
			height = lua.get("height").optint(0);
		} else {
			if (luaSize.istable()) {
				width = luaSize.get(1).optint(0);
				height = luaSize.get(2).optint(width);
			} else {
				width = height = luaSize.toint();
			}
		}

		int x = lua.get("x").optint(0);
		int y = lua.get("y").optint(0);
		if (x == 0 && y == 0) {
			LuaValue luaPosition = lua.get("position");
			if (!luaPosition.isnil()) {
				x = luaPosition.get(1).optint(0);
				y = luaPosition.get(2).optint(0);
			}
		}
		this.x = x;
		this.y = y;
	}

}
