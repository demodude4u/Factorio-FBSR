package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;

public class RoboportRendering extends EntityWithOwnerRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);

		bind.sprite(lua.get("base"));
		bind.animation(lua.get("door_animation_down"));
		bind.animation(lua.get("door_animation_up"));
		bind.circuitConnector(lua.get("circuit_connector"));
	}
}
