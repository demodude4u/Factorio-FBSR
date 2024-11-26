package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

public class SpacePlatformHubRendering extends SimpleEntityRendering {
	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.layeredSprite(lua.get("graphics_set").get("picture"));
	}
}
