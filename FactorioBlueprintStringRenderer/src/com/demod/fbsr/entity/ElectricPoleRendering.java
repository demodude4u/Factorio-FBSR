package com.demod.fbsr.entity;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Layer;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.WirePoint;
import com.demod.fbsr.WirePoint.WireColor;
import com.demod.fbsr.WirePoints;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.fp.FPRotatedSprite;
import com.demod.fbsr.fp.FPWireConnectionPoint;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;

@EntityType("electric-pole")
public class ElectricPoleRendering extends EntityWithOwnerRendering {

	private FPRotatedSprite protoPictures;
	private List<FPWireConnectionPoint> protoConnectionPoints;

	public double computePrincipalOrientation(List<MapPosition> points) {
		if (points.isEmpty()) {
			return 0;
		}

		if (points.size() == 1) {
			MapPosition p = points.get(0);
			double rad = Math.atan2(p.getY(), p.getX()) + (Math.PI / 2.0);
			if (rad < 0) {
				rad += Math.PI * 2;
			}
			return rad / (Math.PI * 2);
		}

		double meanX = 0, meanY = 0;
		for (MapPosition p : points) {
			meanX += p.getX();
			meanY += p.getY();
		}
		meanX /= points.size();
		meanY /= points.size();

		double Sxx = 0, Syy = 0, Sxy = 0;
		for (MapPosition p : points) {
			double dx = p.getX() - meanX;
			double dy = p.getY() - meanY;
			Sxx += dx * dx;
			Syy += dy * dy;
			Sxy += dx * dy;
		}

		double rad = 0.5 * Math.atan2(2 * Sxy, Sxx - Syy) + (Math.PI / 2.0);
		if (rad < 0) {
			rad += Math.PI * 2;
		}
		return rad / (Math.PI * 2);
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		super.initAtlas(register);

		protoPictures.getDefs(register);
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		// XXX strange that I have to force back_equals_front to be true
		protoPictures = new FPRotatedSprite(profile, prototype.lua().get("pictures"), Optional.of(true));	
		protoConnectionPoints = FPUtils.list(prototype.lua().get("connection_points"), FPWireConnectionPoint::new);

		if (protoPictures.directionCount != protoConnectionPoints.size()) {
			throw new IllegalStateException("Mismatched direction count (" + protoPictures.directionCount + ") and connection point count (" + protoConnectionPoints.size() + ") for " + name);
		}
	}

	@Override
	public void createWireConnector(Consumer<MapRenderable> register, BiConsumer<Integer, WirePoint> registerWirePoint,
			MapEntity entity, List<MapEntity> wired, WorldMap map) {
		super.createWireConnector(register, registerWirePoint, entity, wired, map);

		MapPosition p1 = entity.getPosition();
		List<MapPosition> points = wired.stream().map(t -> {
			MapPosition p2 = t.getPosition();
			return p2.sub(p1);
		}).collect(Collectors.toList());
		double orientation = computePrincipalOrientation(points);
		int index = protoPictures.getIndex(orientation);

		protoPictures.defineSprites(entity.spriteRegister(register, Layer.HIGHER_OBJECT_ABOVE), index);
		FPWireConnectionPoint cp = protoConnectionPoints.get(index);
		registerWirePoint.accept(1, WirePoint.fromConnectionPoint(WireColor.RED, cp, entity));
		registerWirePoint.accept(2, WirePoint.fromConnectionPoint(WireColor.GREEN, cp, entity));
		registerWirePoint.accept(5, WirePoint.fromConnectionPoint(WireColor.COPPER, cp, entity));
	}
}
