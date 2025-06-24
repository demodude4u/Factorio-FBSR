package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.EntityType;

@EntityType("radar")
public class RadarRendering extends EntityWithOwnerRendering {
	private static final double ORIENTATION = 0.375;

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);
		
		bind.rotatedSprite(lua.get("pictures")).orientation(ORIENTATION);
		bind.circuitConnector(lua.get("circuit_connector"));
	}
}
