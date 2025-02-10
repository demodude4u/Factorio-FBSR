package com.demod.fbsr.entity;

import java.awt.geom.Point2D;

import com.demod.fbsr.Direction;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;

public class CurvedRailRendering extends RailRendering {

	// TODO make sure the path ends support transition to half diagonals

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

	public CurvedRailRendering() {
		this(false);
	}

	public CurvedRailRendering(boolean elevated) {
		super(elevated);
	}

	@Override
	public void populateWorldMap(WorldMap map, BSEntity entity) {
		Point2D.Double pos = entity.position.createPoint();
		Direction dir = entity.direction;

		int[][] points = pathEnds[dir.ordinal()];
		Point2D.Double p1 = new Point2D.Double(pos.x + points[0][0], pos.y + points[0][1]);
		Direction d1 = Direction.values()[points[0][2]];
		Point2D.Double p2 = new Point2D.Double(pos.x + points[1][0], pos.y + points[1][1]);
		Direction d2 = Direction.values()[points[1][2]];
		Point2D.Double cp1 = d1.offset(p1, 0.5);
		Point2D.Double cp2 = d2.offset(p2, 0.5);

		map.setRailEdge(p1, d1, cp1, d1.back(), false);
		map.setRailEdge(cp1, d1, cp2, d2, true);
		map.setRailEdge(cp2, d2.back(), p2, d2, false);
	}
}
