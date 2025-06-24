package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.EntityType;

@EntityType("assembling-machine")
public class AssemblingMachineRendering extends CraftingMachineRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);

		bind.circuitConnector4Way(lua.get("circuit_connector"));
	}
}
