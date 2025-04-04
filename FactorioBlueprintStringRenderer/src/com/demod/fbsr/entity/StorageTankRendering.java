package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;

public class StorageTankRendering extends SimpleEntityRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.sprite(lua.get("pictures").get("window_background"));
		bind.sprite4Way(lua.get("pictures").get("picture"));
		bind.circuitConnector4Way(lua.get("circuit_connector"));
		bind.fluidBox(lua.get("fluid_box"));
	}
}
