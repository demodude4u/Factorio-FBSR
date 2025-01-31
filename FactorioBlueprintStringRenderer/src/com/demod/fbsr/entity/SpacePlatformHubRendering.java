package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

public class SpacePlatformHubRendering extends CargoBayConnectionsRendering {
	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bindCargoStationParameters(bind, lua.get("cargo_station_parameters"));
	}
}
