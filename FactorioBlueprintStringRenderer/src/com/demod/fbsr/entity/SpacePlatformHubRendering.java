package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;

public class SpacePlatformHubRendering extends CargoBayConnectionsRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bindCargoStationParameters(bind, lua.get("cargo_station_parameters"));
	}
}
