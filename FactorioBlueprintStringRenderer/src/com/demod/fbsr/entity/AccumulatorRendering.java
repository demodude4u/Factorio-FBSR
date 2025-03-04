package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;

public class AccumulatorRendering extends SimpleEntityRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.sprite(lua.get("chargable_graphics").get("picture"));
		bind.circuitConnector(lua.get("circuit_connector"));
	}
}
