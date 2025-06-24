package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.EntityType;

@EntityType("burner-generator")
public class BurnerGeneratorRendering extends EntityWithOwnerRendering {

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);

		LuaValue luaIdleAnimation = prototype.lua().get("idle_animation");
		if (!luaIdleAnimation.isnil()) {
			bind.animation4Way(luaIdleAnimation);
		} else {
			bind.animation4Way(prototype.lua().get("animation"));
		}
	}
}
