package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;

public class SolarPanelRendering extends EntityWithOwnerRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);
		
		bind.spriteVariations(lua.get("picture"));
	}
}
