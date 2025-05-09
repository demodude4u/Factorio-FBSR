package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;

public class LandMineRendering extends EntityWithOwnerRendering {

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);
		
		bind.sprite(lua.get("picture_safe"));
	}
}
