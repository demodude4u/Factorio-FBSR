package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.fakelua.LuaValue;

public class BurnerGeneratorRendering extends SimpleEntityRendering {

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		LuaValue luaIdleAnimation = prototype.lua().get("idle_animation");
		if (!luaIdleAnimation.isnil()) {
			bind.animation4Way(luaIdleAnimation);
		} else {
			bind.animation4Way(prototype.lua().get("animation"));
		}
	}
}
