package com.demod.fbsr.entity;

import java.util.ArrayList;
import java.util.List;

import com.demod.fbsr.Dir16;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.entity.CurvedRailARendering.CurvedRailSpliner;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapPosition3D;
import com.demod.fbsr.map.MapRect;

@EntityType("curved-rail-b")
public class CurvedRailBRendering extends RailRendering {

	public static final double LENGTH = 5.077891568;
	public static final double[] X_COEF = { 0.9999296427835839, -0.4336250111930321, -0.0343689574670654, 0.0006729125223222 };
	public static final double[] Y_COEF = { 2.0000700817657306, -0.9029070058349963, 0.0190210273499390, 0.0007210510062280 };

	private final List<RailDef> railDefs;

	public CurvedRailBRendering() {
		this(false);
	}

	public CurvedRailBRendering(boolean elevated) {
		super(elevated, Dir16.N, Dir16.NE, Dir16.E, Dir16.SE, Dir16.S, Dir16.SW, Dir16.W, Dir16.NW);

		railDefs = createRailDefs();
	}

	private List<RailDef> createRailDefs() {
		RailDef defNNW_NW = new RailDef(1, 2, "SSE", elevated, -2, -2, "NW", elevated, 
			new CurvedRailSpliner(LENGTH, X_COEF, Y_COEF, false, false, false), //
			group(-0.5, 1.5, "NNW", elevated, 1.5, 0.5, "SSE", elevated, 0.5, -0.5, "SSE", elevated), //
			group(-2.5, -0.5, "NW", elevated, -0.5, -2.5, "SE", elevated));

		List<RailDef> defs = new ArrayList<>();
		defs.add(defNNW_NW); // 0
		defs.add(defNNW_NW.flipX().withSpliner(new CurvedRailSpliner(LENGTH, X_COEF, Y_COEF, true, false, false)));  // 1
		defs.add(defNNW_NW.rotate90().withSpliner(new CurvedRailSpliner(LENGTH, X_COEF, Y_COEF, false, false, true)));  // 2
		defs.add(defNNW_NW.rotate90().flipY().withSpliner(new CurvedRailSpliner(LENGTH, X_COEF, Y_COEF, false, true, true)));   // 3
		defs.add(defNNW_NW.flipX().flipY().withSpliner(new CurvedRailSpliner(LENGTH, X_COEF, Y_COEF, true, true, false)));   // 4
		defs.add(defNNW_NW.flipY().withSpliner(new CurvedRailSpliner(LENGTH, X_COEF, Y_COEF, false, true, false)));  // 5
		defs.add(defNNW_NW.rotate90().flipX().flipY().withSpliner(new CurvedRailSpliner(LENGTH, X_COEF, Y_COEF, true, true, true)));    // 6
		defs.add(defNNW_NW.rotate90().flipX().withSpliner(new CurvedRailSpliner(LENGTH, X_COEF, Y_COEF, true, false, true)));   // 7
		return defs;
	}

	@Override
	protected RailDef getRailDef(MapEntity entity) {
		return railDefs.get(entity.getDirection().ordinal());
	}
}
