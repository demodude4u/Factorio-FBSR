package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;

public class LightningAttractorRendering extends SimpleEntityRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.sprite(lua.get("chargable_graphics").get("picture"));
	}
}
