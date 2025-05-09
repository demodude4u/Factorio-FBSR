package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;

public class CargoBayRendering extends CargoBayConnectionsRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);
		
		bindHatchDefinitions(bind, lua.get("hatch_definitions"));
	}
}
