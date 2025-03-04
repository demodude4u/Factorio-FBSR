package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;

public class OffshorePumpRendering extends SimpleEntityRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.animation4Way(lua.get("graphics_set").get("animation"));
		bind.circuitConnector4Way(lua.get("circuit_connector"));
		bind.fluidBox(lua.get("fluid_box"));
	}
}
