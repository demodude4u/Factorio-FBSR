package com.demod.fbsr.entity;

import java.awt.geom.Point2D;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;

public class StorageTankRendering extends SimpleEntityRendering<BSEntity> {

	public static final int[][][] storageTankPipes = //
			new int[/* NESW */][/* Points */][/* XY */] { //
					{ { 1, 1 }, { -1, -1 } }, // North
					{ { 1, -1 }, { -1, 1 } }, // East
					{ { 1, 1 }, { -1, -1 } }, // South
					{ { 1, -1 }, { -1, 1 } },// West
			};

	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.sprite4Way(lua.get("pictures").get("picture"));
		bind.circuitConnector4Way(lua.get("circuit_connector"));
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BSEntity entity) {
		// FIXME maybe should use the fluid box

		Point2D.Double position = entity.position.createPoint();

		int[][] pipePoints = storageTankPipes[entity.direction.cardinal()];

		for (int[] point : pipePoints) {
			map.setPipe(new Point2D.Double(position.x + point[0], position.y + point[1]));
		}
	}
}
