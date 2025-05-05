package com.demod.fbsr.entity;

import java.util.List;

import com.demod.fbsr.Dir16;
import com.demod.fbsr.Direction;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapPosition3D;
import com.demod.fbsr.map.MapRect;
import com.demod.fbsr.map.MapRect3D;
import com.google.common.collect.ImmutableList;

public class RailRampRendering extends RailRendering {

	public static class RampRailSpliner extends RailSpliner {

		private Direction dir;

		public RampRailSpliner(Direction dir) {
			this.dir = dir;
		}

		@Override
		public double calculateSplineLength(RailDef rail) {
			int steps = 1024;
			double length = 0;
			for (int i=0;i<steps;i++) {
				double t0 = i / (double)steps;
				double t1 = (i+1) / (double)steps;
				double x0 = calculatePosition(t0);
				double x1 = calculatePosition(t1);
				double y0 = calculateHeight(t0);
				double y1 = calculateHeight(t1);
				length += Math.sqrt((x1-x0)*(x1-x0) + (y1-y0)*(y1-y0));
			}
			return length;
		}

		private double calculatePosition(double t) {
			return 8 - 16 * t;
		}

		private double calculateHeight(double t) {
			double u = (t-0.5) * 1.14285714;
			u = Math.min(0.5, Math.max(-0.5, u));
			double v = (1 + Math.sin(u * Math.PI)) / 2.0;
			return v * ELEVATED_HEIGHT;
		}

		@Override
		public MapPosition3D splinePoint(RailDef rail, double distance) {
			double t = distance / rail.length;
			double y = calculatePosition(t);
			MapPosition pos = dir.rotate(MapPosition.byUnit(0, y));
			double height = calculateHeight(t);
			return MapPosition3D.by2DUnit(pos, height);
		}
	};

	private final List<RailDef> railDefs;

	public RailRampRendering() {
		super(false, Dir16.N, Dir16.E, Dir16.S, Dir16.W);

		railDefs = createRailDefs();
	}

	private List<RailDef> createRailDefs() {
		RailDef defN = new RailDef(0, 8, "S", false, 0, -8, "N", true, new RampRailSpliner(Direction.NORTH));
		return ImmutableList.of(//
				defN, //
				defN.rotate90().withSpliner(new RampRailSpliner(Direction.EAST)), //
				defN.flipY().withSpliner(new RampRailSpliner(Direction.SOUTH)), //
				defN.rotate90().flipX().withSpliner(new RampRailSpliner(Direction.WEST)) //
		);
	}

	@Override
	protected RailDef getRailDef(MapEntity entity) {
		return railDefs.get(entity.getDirection().cardinal());
	}

	@Override
	protected MapRect3D computeBounds() {
		return defaultComputeBounds();
	}

	@Override
	public MapRect3D getDrawBounds(MapEntity entity) {
		return entity.getDirection().rotate(drawBounds).shift(entity.getPosition());
	}
}
