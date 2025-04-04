package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;

public class CarRendering extends SimpleEntityRendering {

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.rotatedAnimationLimited(lua.get("animation"), 8);
		bind.rotatedAnimationLimited(lua.get("turret_animation"), 8);
	}

}
