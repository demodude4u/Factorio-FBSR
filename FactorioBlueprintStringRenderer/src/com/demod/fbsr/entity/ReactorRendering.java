package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.Direction;
import com.demod.fbsr.WorldMap;

public class ReactorRendering extends SimpleEntityRendering {
	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.sprite(lua.get("lower_layer_picture"));
		bind.sprite(lua.get("picture"));
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity) {
		for (Direction dir : Direction.values()) {
			map.setHeatPipe(dir.offset(entity.getPosition(), 2));
		}
	}
}
