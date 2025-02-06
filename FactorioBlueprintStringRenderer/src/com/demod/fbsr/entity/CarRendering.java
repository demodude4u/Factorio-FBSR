package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.bs.BSEntity;

public class CarRendering extends SimpleEntityRendering<BSEntity> {

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.rotatedAnimation(lua.get("animation"));
		bind.rotatedAnimation(lua.get("turret_animation"));
	}

}
