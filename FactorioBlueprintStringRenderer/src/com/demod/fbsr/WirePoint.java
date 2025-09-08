package com.demod.fbsr;

import java.awt.Color;
import java.util.Optional;

import com.demod.fbsr.fp.FPVector;
import com.demod.fbsr.fp.FPWireConnectionPoint;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;

public class WirePoint {
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

    private final WireColor color;
    private final MapPosition position;
    private final MapPosition shadow;

    public WirePoint(WireColor color, MapPosition position, MapPosition shadow) {
        this.color = color;
        this.position = position;
        this.shadow = shadow;
    }

    public static WirePoint fromConnectionPoint(WireColor color, FPWireConnectionPoint point, MapEntity entity) {
        Optional<FPVector> wire;
        Optional<FPVector> shadow;
        switch (color) {
            case RED:
                wire = point.wire.red;
                shadow = point.shadow.red;
                break;
            case GREEN:
                wire = point.wire.green;
                shadow = point.shadow.green;
                break;
            case COPPER:
                wire = point.wire.copper;
                shadow = point.shadow.copper;
                break;
            default:
                return null;
        }
        if (!wire.isPresent()) {
            throw new IllegalArgumentException("No wire of color " + color + " in connection point");
        }
        return fromConnectionPoint(color, wire.get(), shadow, entity);
    }

    public static WirePoint fromConnectionPoint(WireColor color, FPVector wire, Optional<FPVector> shadow, MapEntity entity) {
        MapPosition entityPos = entity.getPosition();
        MapPosition position = entityPos.addUnit(wire);
        MapPosition shadowPosition = shadow.map(s -> entityPos.addUnit(s)).orElse(position);
        return new WirePoint(color, position, shadowPosition);
    }

    public WireColor getColor() {
        return color;
    }

    public MapPosition getPosition() {
        return position;
    }

    public MapPosition getShadow() {
        return shadow;
    }
}
