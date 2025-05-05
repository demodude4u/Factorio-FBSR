package com.demod.fbsr.entity;

import java.util.ArrayList;
import java.util.List;

import com.demod.fbsr.Dir16;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapPosition3D;
import com.demod.fbsr.map.MapRect;

public class CurvedRailARendering extends RailRendering {

	public static final double LENGTH = 5.132284556;
	public static final double[] X_COEF = { -0.0001006957601695, 0.0006303089061514, -0.0390775228531406, 0.0001929289349940 };
	public static final double[] Y_COEF = { 2.0000180760089301, -1.0001059650840689, 0.0000918676400977, 0.0009646884174780 };

	public static class CurvedRailSpliner extends RailSpliner {

		private final double length;
		private final double[] xCoef;
		private final double[] yCoef;
		private final boolean flipX;
		private final boolean flipY;
		private final boolean rotate90;

		public CurvedRailSpliner(double length, double[] xCoef, double[] yCoef, boolean flipX, boolean flipY, boolean rotate90) {
			this.length = length;
			this.xCoef = xCoef;
			this.yCoef = yCoef;
			this.flipX = flipX;
			this.flipY = flipY;
			this.rotate90 = rotate90;
		}

		@Override
		public double calculateSplineLength(RailDef rail) {
			return this.length;
		}

		@Override
		public MapPosition3D splinePoint(RailDef rail, double distance) {
			if (distance < 0.0 || distance > this.length) {
				throw new IllegalArgumentException("Distance out of bounds: " + distance);
			}

			double d = distance;
			double d2 = d * d;
			double d3 = d2 * d;

			double x = xCoef[0] + d * xCoef[1] + d2 * xCoef[2] + d3 * xCoef[3];
			double y = yCoef[0] + d * yCoef[1] + d2 * yCoef[2] + d3 * yCoef[3];

			if (rotate90) {
				double temp = x;
				x = -y;
				y = temp;
			}
			if (flipX) {
				x = -x;
			}
			if (flipY) {
				y = -y;
			}

			return MapPosition3D.byUnit(x, y, rail.A.elevated ? ELEVATED_HEIGHT : 0);
		}
	}

	private final List<RailDef> railDefs;

	public CurvedRailARendering() {
		this(false);
	}

	public CurvedRailARendering(boolean elevated) {
		super(elevated, Dir16.N, Dir16.NE, Dir16.E, Dir16.SE, Dir16.S, Dir16.SW, Dir16.W, Dir16.NW);

		railDefs = createRailDefs();
	}

	private List<RailDef> createRailDefs() {
		RailDef defN_NNW = new RailDef(0, 2, "S", elevated, -1, -3, "NNW", elevated, 
			new CurvedRailSpliner(LENGTH, X_COEF, Y_COEF, false, false, false), //
			group(-1.5, 1.5, "N", elevated, 1.5, 1.5, "S", elevated), //
			group(-1.5, -1.5, "NNW", elevated, 0.5, -2.5, "SSE", elevated));

		List<RailDef> defs = new ArrayList<>();
		defs.add(defN_NNW); // 0
		defs.add(defN_NNW.flipX().withSpliner(new CurvedRailSpliner(LENGTH, X_COEF, Y_COEF, true, false, false)));  // 1
		defs.add(defN_NNW.rotate90().withSpliner(new CurvedRailSpliner(LENGTH, X_COEF, Y_COEF, false, false, true)));  // 2
		defs.add(defN_NNW.rotate90().flipY().withSpliner(new CurvedRailSpliner(LENGTH, X_COEF, Y_COEF, false, true, true)));   // 3
		defs.add(defN_NNW.flipX().flipY().withSpliner(new CurvedRailSpliner(LENGTH, X_COEF, Y_COEF, true, true, false)));   // 4
		defs.add(defN_NNW.flipY().withSpliner(new CurvedRailSpliner(LENGTH, X_COEF, Y_COEF, false, true, false)));  // 5
		defs.add(defN_NNW.rotate90().flipX().flipY().withSpliner(new CurvedRailSpliner(LENGTH, X_COEF, Y_COEF, true, true, true)));    // 6
		defs.add(defN_NNW.rotate90().flipX().withSpliner(new CurvedRailSpliner(LENGTH, X_COEF, Y_COEF, true, false, true)));   // 7
		return defs;
	}

	@Override
	protected RailDef getRailDef(MapEntity entity) {
		return railDefs.get(entity.getDirection().ordinal());
	}
}
