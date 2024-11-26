package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

public class ThrusterRendering extends SimpleEntityRendering {
	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.animation4Way(lua.get("graphics_set").get("animation"));
	}
}
