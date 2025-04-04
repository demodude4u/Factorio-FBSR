package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;

public class SolarPanelRendering extends SimpleEntityRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.spriteVariations(lua.get("picture"));
	}
}
