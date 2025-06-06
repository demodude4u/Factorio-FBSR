package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;

public class OffshorePumpRendering extends EntityWithOwnerRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);
		
		bind.animation4Way(lua.get("graphics_set").get("animation"));
		bind.circuitConnector4Way(lua.get("circuit_connector"));
		bind.fluidBox(lua.get("fluid_box"));
	}
}
