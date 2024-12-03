package com.demod.fbsr;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.demod.fbsr.fp.FPVector;
import com.demod.fbsr.fp.FPWireConnectionPoint;
import com.demod.fbsr.fp.FPWirePosition;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

public class WirePoints {
	public static enum WireColor {
		COPPER(new Color(0xffa87c14)), //
		RED(Color.red.darker()), //
		GREEN(Color.green.darker()),//
		;

		private final Color color;

		private WireColor(Color color) {
			this.color = color;
		}

		public Color getColor() {
			return color;
		}
	}

	public static class WirePoint {
		private final WireColor color;
		private final Point2D.Double position;
		private final Point2D.Double shadow;

		public WirePoint(WireColor color, Point2D.Double position, Point2D.Double shadow) {
			this.color = color;
			this.position = position;
			this.shadow = shadow;
		}

		public WireColor getColor() {
			return color;
		}

		public Point2D.Double getPosition() {
			return position;
		}

		public Point2D.Double getShadow() {
			return shadow;
		}
	}

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
			dirOffsets.add(wireSelect.apply(point.wire).get());
			dirShadowOffsets.add(wireSelect.apply(point.shadow).get());
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

	public WirePoint getPoint(Point2D.Double position, double orientation) {
		int index;
		if (backEqualsFront) {
			index = ((int) Math.round(orientation * dirOffsets.size() * 2)) % dirOffsets.size();
		} else {
			index = ((int) Math.round(orientation * dirOffsets.size())) % dirOffsets.size();
		}
		FPVector offset = dirOffsets.get(index);
		FPVector shadowOffset = dirShadowOffsets.get(index);
		Point2D.Double pos = new Point2D.Double(position.x + offset.x, position.y + offset.y);
		Point2D.Double shadowPos = new Point2D.Double(position.x + shadowOffset.x, position.y + shadowOffset.y);
		return new WirePoint(color, pos, shadowPos);
	}

}
