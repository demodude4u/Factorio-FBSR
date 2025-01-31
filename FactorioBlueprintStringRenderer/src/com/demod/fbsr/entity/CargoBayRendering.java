package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

public class CargoBayRendering extends CargoBayConnectionsRendering {
	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		// TODO there is both graphics_set and platform_graphics_set, depending on
		// whether on a space platform or not

		bind.layeredSprite(lua.get("platform_graphics_set").get("picture"));

		bindHatchDefinitions(bind, lua.get("hatch_definitions"));
	}
}
