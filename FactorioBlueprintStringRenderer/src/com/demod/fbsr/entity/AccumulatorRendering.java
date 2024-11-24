package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

public class AccumulatorRendering extends SimpleEntityRendering {
	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.sprite(lua.get("chargable_graphics").get("picture"));
		bind.circuitConnector(lua.get("circuit_connector"));
	}
}
