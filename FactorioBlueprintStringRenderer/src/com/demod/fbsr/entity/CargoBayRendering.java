package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;

public class CargoBayRendering extends CargoBayConnectionsRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bindHatchDefinitions(bind, lua.get("hatch_definitions"));
	}
}
