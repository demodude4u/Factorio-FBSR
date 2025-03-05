package com.demod.fbsr.entity;

import com.demod.fbsr.Direction;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;

public class LegacyCurvedRailRendering extends RailRendering {

	private static final int[][][] pathEnds = //
			new int[/* dir */][/* points */][/* x,y,dir */] { //
					{ { 1, 4, 0 }, { -2, -3, 3 } }, // N
					{ { -1, 4, 0 }, { 2, -3, 5 } }, // NE
					{ { -4, 1, 2 }, { 3, -2, 5 } }, // E
					{ { -4, -1, 2 }, { 3, 2, 7 } }, // SE
					{ { -1, -4, 4 }, { 2, 3, 7 } }, // S
					{ { 1, -4, 4 }, { -2, 3, 1 } }, // SW
					{ { 4, -1, 6 }, { -3, 2, 1 } }, // W
					{ { 4, 1, 6 }, { -3, -2, 3 } }, // NW
			};

	public LegacyCurvedRailRendering() {
		super(false);
	}

	@Override
	public void populateWorldMap(WorldMap map, MapEntity entity) {
		MapPosition pos = entity.getPosition();
		Direction dir = entity.getDirection();

		int[][] points = pathEnds[dir.ordinal()];
		MapPosition p1 = pos.addUnit(points[0][0], points[0][1]);
		Direction d1 = Direction.values()[points[0][2]];
		MapPosition p2 = pos.addUnit(points[1][0], points[1][1]);
		Direction d2 = Direction.values()[points[1][2]];
		MapPosition cp1 = d1.offset(p1, 0.5);
		MapPosition cp2 = d2.offset(p2, 0.5);

		map.setRailEdge(p1, d1, cp1, d1.back(), false);
		map.setRailEdge(cp1, d1, cp2, d2, true);
		map.setRailEdge(cp2, d2.back(), p2, d2, false);
	}
}