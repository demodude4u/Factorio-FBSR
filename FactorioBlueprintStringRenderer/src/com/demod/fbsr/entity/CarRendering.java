package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

import com.demod.fbsr.bs.BSEntity;

public class CarRendering extends SimpleEntityRendering<BSEntity> {

	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.rotatedAnimation(lua.get("animation"));
		bind.rotatedAnimation(lua.get("turret_animation"));
	}

}
