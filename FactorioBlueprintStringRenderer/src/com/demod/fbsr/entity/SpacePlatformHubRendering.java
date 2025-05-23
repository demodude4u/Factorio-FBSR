package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;

public class SpacePlatformHubRendering extends CargoBayConnectionsRendering {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);
		
		bind.circuitConnector(lua.get("circuit_connector"));

		bindCargoStationParameters(bind, lua.get("cargo_station_parameters"));
	}
}
