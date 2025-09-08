package com.demod.fbsr;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.demod.fbsr.fp.FPVector;
import com.demod.fbsr.fp.FPWireConnectionPoint;
import com.demod.fbsr.fp.FPWirePosition;
import com.demod.fbsr.map.MapPosition;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

public class WirePoints {

	public static final Set<Integer> VALID_SIZES = ImmutableSet.of(1, 4, 8, 16);

	// XXX is there a better way to calculate with back_equals_front?
	public static WirePoints fromWireConnectionPoints(List<FPWireConnectionPoint> points, WireColor color,
			boolean backEqualsFront) {
		List<FPVector> dirOffsets = new ArrayList<>();
		List<FPVector> dirShadowOffsets = new ArrayList<>();
		Function<FPWirePosition, Optional<FPVector>> wireSelect = wp -> {
			switch (color) {
			case RED:
				return wp.red;
			case GREEN:
				return wp.green;
			case COPPER:
				return wp.copper;
			}
			return Optional.empty();
		};
		for (FPWireConnectionPoint point : points) {
			dirOffsets.add(wireSelect.apply(point.wire).orElseGet(() -> new FPVector(0, 0)));
			dirShadowOffsets.add(wireSelect.apply(point.shadow).orElseGet(() -> new FPVector(0, 0)));
		}
		return new WirePoints(color, dirOffsets, dirShadowOffsets, backEqualsFront);
	}

	private final WireColor color;
	private final List<FPVector> dirOffsets;
	private final List<FPVector> dirShadowOffsets;
	private final boolean backEqualsFront;

	public WirePoints(WireColor color, List<FPVector> dirOffsets, List<FPVector> dirShadowOffsets,
			boolean backEqualsFront) {
		Preconditions.checkArgument(VALID_SIZES.contains(dirOffsets.size()),
				"Invalid size count: " + dirOffsets.size());
		this.color = color;
		this.dirOffsets = dirOffsets;
		this.dirShadowOffsets = dirShadowOffsets;
		this.backEqualsFront = backEqualsFront;
	}

	public WireColor getColor() {
		return color;
	}

	public WirePoint getPoint(MapPosition position, double orientation) {
		int index;
		if (backEqualsFront) {
			index = ((int) Math.round(orientation * dirOffsets.size() * 2)) % dirOffsets.size();
		} else {
			index = ((int) Math.round(orientation * dirOffsets.size())) % dirOffsets.size();
		}
		FPVector offset = dirOffsets.get(index);
		FPVector shadowOffset = dirShadowOffsets.get(index);
		return new WirePoint(color, position.addUnit(offset), position.addUnit(shadowOffset));
	}

}
