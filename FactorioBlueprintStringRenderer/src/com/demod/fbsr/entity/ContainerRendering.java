package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

public class ContainerRendering extends SimpleEntityRendering {
	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.sprite(lua.get("picture"));
	}
}
