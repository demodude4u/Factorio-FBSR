package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;

public class CarRendering extends EntityWithOwnerRendering {

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);

		bind.rotatedAnimationLimited(lua.get("animation"), 8);
		bind.rotatedAnimationLimited(lua.get("turret_animation"), 8);
	}

}
