package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;

public class LinkedContainerRendering extends SimpleEntityRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.sprite(lua.get("picture"));
		bind.circuitConnector(lua.get("circuit_connector"));
	}
}
