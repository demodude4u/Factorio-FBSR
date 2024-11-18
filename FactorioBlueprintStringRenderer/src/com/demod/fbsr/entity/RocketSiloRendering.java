package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

import com.demod.fbsr.Renderer.Layer;

public class RocketSiloRendering extends SimpleEntityRendering {
	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.sprite(lua.get("shadow_sprite"));
		bind.sprite(lua.get("door_front_sprite"));
		bind.sprite(lua.get("door_back_sprite"));
		bind.sprite(lua.get("base_day_sprite")).layer(Layer.ENTITY2);
	}
}
