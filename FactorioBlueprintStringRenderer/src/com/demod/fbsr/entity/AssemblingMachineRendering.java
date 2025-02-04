package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

public class AssemblingMachineRendering extends CraftingMachineRendering {
	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		super.defineEntity(bind, lua);

		bind.circuitConnector4Way(lua.get("circuit_connector"));
	}
}
