package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

public class RoboportRendering extends SimpleEntityRendering {
	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.sprite(lua.get("base"));
		bind.animation(lua.get("door_animation_down"));
		bind.animation(lua.get("door_animation_up"));
	}
}
