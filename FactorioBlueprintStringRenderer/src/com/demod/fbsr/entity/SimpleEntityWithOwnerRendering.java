package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.fakelua.LuaValue;

public class SimpleEntityWithOwnerRendering extends EntityWithOwnerRendering {

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		LuaValue luaGraphics;

		if (!(luaGraphics = lua.get("pictures")).isnil()) {
			bind.spriteVariations(luaGraphics);

		} else if (!(luaGraphics = lua.get("picture")).isnil()) {
			bind.sprite4Way(luaGraphics);

		} else if (!(luaGraphics = lua.get("animation")).isnil()) {
			bind.animationVariations(luaGraphics);
		}
	}

}
