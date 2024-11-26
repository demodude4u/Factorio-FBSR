package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

public class AsteroidCollectorRendering extends SimpleEntityRendering {

	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.animation4Way(lua.get("graphics_set").get("animation"));
		bind.circuitConnector4Way(lua.get("circuit_connector"));
	}

}
