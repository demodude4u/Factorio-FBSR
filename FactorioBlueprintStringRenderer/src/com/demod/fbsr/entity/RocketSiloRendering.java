package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.Layer;

public class RocketSiloRendering extends SimpleEntityRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.animation(lua.get("satellite_animation"));
		bind.sprite(lua.get("shadow_sprite"));
		bind.sprite(lua.get("door_front_sprite"));
		bind.sprite(lua.get("door_back_sprite"));
		bind.sprite(lua.get("base_day_sprite")).layer(Layer.HIGHER_OBJECT_UNDER);
	}
}
