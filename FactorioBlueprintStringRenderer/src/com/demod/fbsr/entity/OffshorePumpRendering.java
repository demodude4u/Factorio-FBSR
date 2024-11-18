package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.WorldMap;

public class OffshorePumpRendering extends SimpleEntityRendering {
	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.animation4Way(lua.get("graphics_set").get("animation"));
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity) {
		map.setPipe(entity.getPosition(), entity.getDirection().back());
	}
}
