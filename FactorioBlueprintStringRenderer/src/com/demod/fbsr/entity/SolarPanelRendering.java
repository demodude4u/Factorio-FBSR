package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.EntityType;

@EntityType("solar-panel")
public class SolarPanelRendering extends EntityWithOwnerRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);
		
		bind.spriteVariations(lua.get("picture"));
	}
}
