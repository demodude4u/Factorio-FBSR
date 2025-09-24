package com.demod.fbsr.entity;

import com.demod.fbsr.EntityType;
import com.demod.fbsr.bind.Bindings;
import com.demod.factorio.fakelua.LuaTable;

@EntityType("accumulator")
public class AccumulatorRendering extends EntityWithOwnerRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);

		bind.sprite(lua.get("chargable_graphics").get("picture"));
		bind.circuitConnector(lua.get("circuit_connector"));
	}
}
