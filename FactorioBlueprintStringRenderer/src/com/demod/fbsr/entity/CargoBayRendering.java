package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

public class CargoBayRendering extends CargoBayConnectionsRendering {
	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bindHatchDefinitions(bind, lua.get("hatch_definitions"));
	}
}
