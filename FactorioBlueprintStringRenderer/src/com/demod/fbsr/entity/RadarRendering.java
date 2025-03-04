package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;

public class RadarRendering extends SimpleEntityRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.rotatedSprite(lua.get("pictures"));
		bind.circuitConnector(lua.get("circuit_connector"));
	}
}
