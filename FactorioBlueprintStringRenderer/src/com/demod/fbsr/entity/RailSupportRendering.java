package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

public class RailSupportRendering extends SimpleEntityRendering {
	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.rotatedSprite(lua.get("graphics_set").get("structure"));
	}

}
