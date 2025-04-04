package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;

public class LabRendering extends SimpleEntityRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.animation(lua.get("off_animation"));
	}
}
