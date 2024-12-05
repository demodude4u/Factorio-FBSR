package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

import com.demod.fbsr.bs.BSEntity;

public class RailSupportRendering extends SimpleEntityRendering<BSEntity> {
	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.rotatedSprite(lua.get("graphics_set").get("structure"));
	}

}
