package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

public class LampRendering extends SimpleEntityRendering {

	// TODO check if I can get the color and show the lit colored state

	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.sprite(lua.get("picture_off"));
	}
}
