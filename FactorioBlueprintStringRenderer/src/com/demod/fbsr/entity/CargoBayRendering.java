package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

public class CargoBayRendering extends SimpleEntityRendering {
	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.layeredSprite(lua.get("platform_graphics_set").get("picture"));
	}
}
