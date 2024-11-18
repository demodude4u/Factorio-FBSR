package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

public class SolarPanelRendering extends SimpleEntityRendering {
	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.spriteVariations(lua.get("picture"));
	}
}
