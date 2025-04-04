package com.demod.fbsr.entity;

import com.demod.fbsr.Direction;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;

public class StraightRailRendering extends RailRendering {

	private static final int[][][] pathEnds = //
			new int[/* dir */][/* points */][/* x,y,dir */] { //
					{ { 0, -1, 4 }, { 0, 1, 0 } }, // N
					{ { 0, -1, 3 }, { 1, 0, 7 } }, // NE
					{ { -1, 0, 2 }, { 1, 0, 6 } }, // E
					{ { 0, 1, 1 }, { 1, 0, 5 } }, // SE
					{ { 0, -1, 4 }, { 0, 1, 0 } }, // S
					{ { -1, 0, 3 }, { 0, 1, 7 } }, // SW
					{ { -1, 0, 2 }, { 1, 0, 6 } }, // W
					{ { -1, 0, 1 }, { 0, -1, 5 } }, // NW
			};

	public StraightRailRendering() {
		this(false);
	}

	public StraightRailRendering(boolean elevated) {
		super(elevated);
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

		if (dir.isCardinal()) {
			map.setRailEdge(p1, d1, cp1, d2, false);
			map.setRailEdge(cp1, d1, cp2, d2, false);
			map.setRailEdge(cp2, d1, p2, d2, false);
		} else {
			map.setRailEdge(p1, d1, cp1, d2, false);
			map.setRailEdge(cp1, d1, p2, d2, false);
		}

	}
}
