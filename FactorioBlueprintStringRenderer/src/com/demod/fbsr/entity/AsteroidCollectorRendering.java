package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;

public class AsteroidCollectorRendering extends SimpleEntityRendering {

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.animation4Way(lua.get("graphics_set").get("animation"));
		bind.circuitConnector4Way(lua.get("circuit_connector"));
	}

}
