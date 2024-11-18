package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

public class LandMineRendering extends SimpleEntityRendering {

	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.sprite(lua.get("picture_safe"));
	}
}
