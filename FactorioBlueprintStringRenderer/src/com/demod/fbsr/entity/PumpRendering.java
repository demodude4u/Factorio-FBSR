package com.demod.fbsr.entity;

import java.awt.geom.Point2D;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.Direction;
import com.demod.fbsr.WorldMap;

public class PumpRendering extends SimpleEntityRendering {
	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.animation4Way(lua.get("animations"));
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity) {
		Point2D.Double pos = entity.getPosition();
		Direction dir = entity.getDirection();

		map.setPipe(dir.offset(pos, 0.5), dir);
		map.setPipe(dir.back().offset(pos, 0.5), dir.back());
	}
}
