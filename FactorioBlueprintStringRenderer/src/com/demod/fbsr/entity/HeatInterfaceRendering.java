package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;

public class HeatInterfaceRendering extends SimpleEntityRendering<BSEntity> {

	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.sprite(lua.get("picture"));
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BSEntity entity) {
		super.populateWorldMap(map, dataTable, entity);

		map.setHeatPipe(entity.position.createPoint());
	}
}
