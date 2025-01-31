package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

public class CargoLandingPadRendering extends CargoBayConnectionsRendering {
	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.layeredSprite(lua.get("graphics_set").get("picture"));

		bindCargoStationParameters(bind, lua.get("cargo_station_parameters"));
	}
}
