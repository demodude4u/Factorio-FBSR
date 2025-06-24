package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.Layer;

@EntityType("rocket-silo")
public class RocketSiloRendering extends AssemblingMachineRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);

		bind.animation(lua.get("satellite_animation"));
		bind.sprite(lua.get("shadow_sprite"));
		bind.sprite(lua.get("door_front_sprite"));
		bind.sprite(lua.get("door_back_sprite"));
		bind.sprite(lua.get("base_day_sprite")).layer(Layer.HIGHER_OBJECT_UNDER);
	}
}
