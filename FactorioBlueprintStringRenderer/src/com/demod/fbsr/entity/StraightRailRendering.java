package com.demod.fbsr.entity;

import java.util.List;

import com.demod.fbsr.Dir16;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapPosition3D;
import com.demod.fbsr.map.MapRect;
import com.google.common.collect.ImmutableList;

@EntityType("straight-rail")
public class StraightRailRendering extends RailRendering {

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

	public StraightRailRendering() {
		this(false);
	}

	public StraightRailRendering(boolean elevated) {
		super(elevated, Dir16.N, Dir16.NE, Dir16.E, Dir16.SE);

		railDefs = createRailDefs();
	}

	private List<RailDef> createRailDefs() {
		RailDef defN = new RailDef(0, 1, "S", elevated, 0, -1, "N", elevated, SPLINER, //
				-2, 0, "S", elevated, 2, 0, "N", elevated, //
				group(-1.5, 0.5, "N", elevated, 1.5, 0.5, "S", elevated), //
				group(-1.5, -0.5, "N", elevated, 1.5, -0.5, "S", elevated));

		RailDef defNE = new RailDef(-1, 1, "SW", elevated, 1, -1, "NE", elevated, SPLINER, //
				group(-1.5, -0.5, "NE", elevated, 0.5, 1.5, "SW", elevated), //
				group(-0.5, -1.5, "NE", elevated, 1.5, 0.5, "SW", elevated));

		return ImmutableList.of(defN, defNE, defN.rotate90(), defNE.rotate90());
	}

	@Override
	protected RailDef getRailDef(MapEntity entity) {
		return railDefs.get(entity.getDirection().ordinal());
	}
}
