package com.demod.fbsr.entity;

import java.util.List;

import com.demod.fbsr.Dir16;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapRect;
import com.google.common.collect.ImmutableList;

public class HalfDiagonalRailRendering extends RailRendering {

	public static final RailSpliner SPLINER = StraightRailRendering.SPLINER;

	private final List<RailDef> railDefs;

	public HalfDiagonalRailRendering() {
		this(false);
	}

	public HalfDiagonalRailRendering(boolean elevated) {
		super(elevated, Dir16.N, Dir16.NE, Dir16.E, Dir16.SE);

		railDefs = createRailDefs();
	}

	private List<RailDef> createRailDefs() {
		RailDef defNNW = new RailDef(1, 2, "SSE", elevated, -1, -2, "NNW", elevated, SPLINER, //
				group(-0.5, 1.5, "NNW", elevated, 1.5, 0.5, "SSE", elevated), //
				group(-1.5, -0.5, "NNW", elevated, 0.5, -1.5, "SSE", elevated));

		return ImmutableList.of(defNNW, defNNW.flipX(), defNNW.rotate90(), defNNW.rotate90().flipX());
	}

	@Override
	protected RailDef getRailDef(MapEntity entity) {
		return railDefs.get(entity.getDirection().ordinal());
	}
}
