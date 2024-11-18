package com.demod.fbsr.entity;

import java.awt.geom.Point2D;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.WorldMap;

public class StorageTankRendering extends SimpleEntityRendering {

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
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity) {
		// FIXME maybe should use the fluid box

		Point2D.Double position = entity.getPosition();

		int[][] pipePoints = storageTankPipes[entity.getDirection().cardinal()];

		for (int[] point : pipePoints) {
			map.setPipe(new Point2D.Double(position.x + point[0], position.y + point[1]));
		}
	}
}
