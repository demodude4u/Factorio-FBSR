package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;

public class CarRendering extends SimpleEntityRendering {

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.rotatedAnimation(lua.get("animation"));
		bind.rotatedAnimation(lua.get("turret_animation"));
	}

}
