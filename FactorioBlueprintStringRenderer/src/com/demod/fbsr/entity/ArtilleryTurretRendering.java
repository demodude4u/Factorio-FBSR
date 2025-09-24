package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.bind.Bindings;

@EntityType("artillery-turret")
public class ArtilleryTurretRendering extends EntityWithOwnerRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);

		bind.animation4Way(lua.get("base_picture"));
		bind.rotatedSpriteLimited(lua.get("cannon_barrel_pictures"), 4);
		bind.rotatedSpriteLimited(lua.get("cannon_base_pictures"), 4);
		bind.circuitConnector(lua.get("circuit_connector"));
	}
}
