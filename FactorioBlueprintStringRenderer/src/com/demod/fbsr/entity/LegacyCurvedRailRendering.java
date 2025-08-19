package com.demod.fbsr.entity;

import java.util.List;

import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Dir16;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapPosition3D;
import com.google.common.collect.ImmutableList;

@EntityType("legacy-curved-rail")
public class LegacyCurvedRailRendering extends RailRendering {

	private class LegacyCurveRailSpliner extends RailSpliner {

		private final MapPosition curveCenter;
		private final double startingAngle;
		private final double rotationCoefficient;

		public LegacyCurveRailSpliner(double sx, double sy, double startingAngle, double rotationCorefficient) {
			this.curveCenter = MapPosition.byUnit(sx, sy);
			this.startingAngle = startingAngle;
			this.rotationCoefficient = rotationCorefficient;
		}

		@Override
		public double calculateSplineLength(RailDef rail) {
			return railLength;
		}

		@Override
		public MapPosition3D splinePoint(RailDef rail, double distance) {
			if (distance < straightPartLength) {
				Direction dir = Direction.values()[rail.A.dir.ordinal()/2].back();
				return MapPosition3D.by2DGround(dir.offset(rail.A.pos, distance));
			}
			double angle = (distance - straightPartLength) * rotationCoefficient + startingAngle;
			return MapPosition3D.by2DGround(curveCenter.addUnit(radius * Math.cos(angle), -radius * Math.sin(angle)));
		}
	}
		
	private final List<RailSpliner> spliners;
	private final List<RailDef> railDefs;

	private double straightPartLength;
	private double diagonalPartLength;
	private double turnPartLength;
	private double railLength;
	private double radius;

	public LegacyCurvedRailRendering() {
		super(false, Dir16.N, Dir16.NE, Dir16.E, Dir16.SE, Dir16.S, Dir16.SW, Dir16.W, Dir16.NW);

		spliners = createSpliners();
		railDefs = createRailDefs();
	}

	private List<RailSpliner> createSpliners() {
		// Mostly copied from factorio
		double gauge = 35.0 / 32.0 / 2.0;
		double a = Math.sqrt(2) * gauge * FPUtils.PROJECTION_CONSTANT;
		double b = a / 2.0;
		radius = (3 - b) / (1 - Math.sin(45.0 * Math.PI / 180.0));
		double sx = radius + 1;
		double sy = radius * Math.sin(45 * Math.PI / 180.0) + 1 + b;
		straightPartLength = 8 - sy;
		diagonalPartLength = gauge * FPUtils.PROJECTION_CONSTANT;
		turnPartLength = 2 * radius * Math.PI / 8.0;
		railLength = straightPartLength + diagonalPartLength + turnPartLength;
		double rInv = 1 / radius;

		return ImmutableList.of(
			new LegacyCurveRailSpliner(-sx+2, sy-4, 0/4.0*Math.PI, rInv), // 0
			new LegacyCurveRailSpliner(sx-2, sy-4, 4/4.0*Math.PI, -rInv), // 1
			new LegacyCurveRailSpliner(-sy + 4, -sx + 2, 6 / 4.0 * Math.PI, rInv), // 2
			new LegacyCurveRailSpliner(-sy + 4, sx - 2, 2 / 4.0 * Math.PI, -rInv), // 3
			new LegacyCurveRailSpliner(sx - 2, -sy + 4, 4 / 4.0 * Math.PI, rInv), // 4
			new LegacyCurveRailSpliner(-sx + 2, -sy + 4, 0 / 4.0 * Math.PI, -rInv), // 5
			new LegacyCurveRailSpliner(sy - 4, sx - 2, 2 / 4.0 * Math.PI, rInv), // 6
			new LegacyCurveRailSpliner(sy - 4, -sx + 2, 6 / 4.0 * Math.PI, -rInv) // 7
		);
	}

	private List<RailDef> createRailDefs() {
		RailDef defN_NW = new RailDef(1, 4, "S", false, -2, -3, "NW", false, null, //
				group(-0.5, 3.5, "N", false, 2.5, 3.5, "S", false), //
				group(-2.5, -1.5, "NW", false, -0.5, -3.5, "SE", false));
		
		return ImmutableList.of(
			defN_NW.withSpliner(spliners.get(0)), //0
			defN_NW.flipX().withSpliner(spliners.get(1)), //1
			defN_NW.rotate90().withSpliner(spliners.get(2)), //2
			defN_NW.rotate90().flipY().withSpliner(spliners.get(3)), //3
			defN_NW.flipX().flipY().withSpliner(spliners.get(4)), //4
			defN_NW.flipY().withSpliner(spliners.get(5)), //5
			defN_NW.rotate90().flipX().flipY().withSpliner(spliners.get(6)), //6
			defN_NW.rotate90().flipX().withSpliner(spliners.get(7)) //7
		);
	}

	@Override
	protected RailDef getRailDef(MapEntity entity) {
		return railDefs.get(entity.getDirection().ordinal());
	}
}