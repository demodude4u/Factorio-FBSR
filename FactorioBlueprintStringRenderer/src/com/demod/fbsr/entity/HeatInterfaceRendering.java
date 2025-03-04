package com.demod.fbsr.entity;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;

public class HeatInterfaceRendering extends SimpleEntityRendering {

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.sprite(lua.get("picture"));
	}

	@Override
	public void populateWorldMap(WorldMap map, BSEntity entity) {
		super.populateWorldMap(map, entity);

		map.setHeatPipe(entity.position.createPoint());
	}
}
