package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

import com.demod.fbsr.bs.BSEntity;

public class ArtilleryTurretRendering extends SimpleEntityRendering<BSEntity> {
	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.animation4Way(lua.get("base_picture"));
		bind.rotatedSprite(lua.get("cannon_barrel_pictures"));
		bind.rotatedSprite(lua.get("cannon_base_pictures"));
		bind.circuitConnector(lua.get("circuit_connector"));
	}
}
