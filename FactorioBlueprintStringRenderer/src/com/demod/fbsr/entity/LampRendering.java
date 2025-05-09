package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;

public class LampRendering extends EntityWithOwnerRendering {

	// TODO check if I can get the color and show the lit colored state

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);
		
		bind.sprite(lua.get("picture_off"));
		bind.circuitConnector(lua.get("circuit_connector"));
	}
}
