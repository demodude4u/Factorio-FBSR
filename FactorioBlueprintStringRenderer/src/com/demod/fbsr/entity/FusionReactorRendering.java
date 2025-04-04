package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.fakelua.LuaValue;

public class FusionReactorRendering extends SimpleEntityRendering {

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		LuaValue luaGraphicsSet = lua.get("graphics_set");
		bind.sprite4Way(luaGraphicsSet.get("structure"));

		bind.fluidBox(lua.get("input_fluid_box"));
		bind.fluidBox(lua.get("output_fluid_box"));
	}

	// TODO connectors between adjacent fusion reactors and generators
	// https://factorio.com/blog/post/fff-420

}
