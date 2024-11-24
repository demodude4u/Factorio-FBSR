package com.demod.fbsr.entity;

import java.awt.geom.Point2D;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.fbsr.Direction;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;

public class PumpRendering extends SimpleEntityRendering {
	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.animation4Way(lua.get("animations"));
		bind.circuitConnector4Way(lua.get("circuit_connector"));
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BSEntity entity) {
		Point2D.Double pos = entity.position.createPoint();
		Direction dir = entity.direction;

		map.setPipe(dir.offset(pos, 0.5), dir);
		map.setPipe(dir.back().offset(pos, 0.5), dir.back());
	}
}
