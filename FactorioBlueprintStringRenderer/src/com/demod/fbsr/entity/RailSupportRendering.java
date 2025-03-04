package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;

public class RailSupportRendering extends SimpleEntityRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.rotatedSprite(lua.get("graphics_set").get("structure"));
	}

}
