package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;

public class ArtilleryTurretRendering extends SimpleEntityRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.animation4Way(lua.get("base_picture"));
		bind.rotatedSprite(lua.get("cannon_barrel_pictures"));
		bind.rotatedSprite(lua.get("cannon_base_pictures"));
		bind.circuitConnector(lua.get("circuit_connector"));
	}
}
