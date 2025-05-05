package com.demod.fbsr.entity;

import java.util.List;

import com.demod.fbsr.Direction;
import com.demod.fbsr.Dir16;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapPosition3D;
import com.google.common.collect.ImmutableList;

public class LegacyStraightRailRendering extends RailRendering {

	public static final RailSpliner SPLINER = new RailSpliner() {
		@Override
		public double calculateSplineLength(RailDef rail) {
			return rail.A.pos.distance(rail.B.pos);
		}

		@Override
		public MapPosition3D splinePoint(RailDef rail, double distance) {
			MapPosition p1 = rail.A.pos;
			MapPosition p2 = rail.B.pos;
			
			double length = rail.length;
			double x = p1.getX() + (p2.getX() - p1.getX()) * distance / length;
			double y = p1.getY() + (p2.getY() - p1.getY()) * distance / length;
			
			return MapPosition3D.byUnit(x, y, rail.A.elevated ? ELEVATED_HEIGHT : 0);
		}
	};

	private final List<RailDef> railDefs;

	public LegacyStraightRailRendering() {
		super(false, Dir16.N, Dir16.NE, Dir16.E, Dir16.SE, Dir16.SW, Dir16.NW);

		railDefs = createRailDefs();
	}

	private List<RailDef> createRailDefs() {
		RailDef defN = new RailDef(0, 1, "S", false, 0, -1, "N", false, SPLINER, //
				-2, 0, "S", false, 2, 0, "N", false, //
				group(-1.5, 0.5, "N", false, 1.5, 0.5, "S", false), //
				group(-1.5, -0.5, "N", false, 1.5, -0.5, "S", false));
		RailDef defNW = new RailDef(1,0,"SE",false,0, -1, "NW", false, SPLINER, //
				group(-0.5, 0.5, "NW", false, 1.5, -1.5, "SE", false));

		return ImmutableList.of(
			defN, //0
			defNW, //1
			defN.rotate90(), //2
			defNW.flipY(), //3
			defN, //4
			defNW.flipX().flipY(), //5
			defN.rotate90(), //6
			defNW.flipX() //7
		);
	}

	@Override
	protected RailDef getRailDef(MapEntity entity) {
		return railDefs.get(entity.getDirection().ordinal());
	}
}
