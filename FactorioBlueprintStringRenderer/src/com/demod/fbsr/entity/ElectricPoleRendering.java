package com.demod.fbsr.entity;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Layer;
import com.demod.fbsr.WirePoints;
import com.demod.fbsr.WirePoints.WireColor;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.fp.FPRotatedSprite;
import com.demod.fbsr.fp.FPWireConnectionPoint;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;

public class ElectricPoleRendering extends EntityRendererFactory {

	private FPRotatedSprite protoPictures;

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
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
	}

	@Override
	public void defineWirePoints(BiConsumer<Integer, WirePoints> consumer, LuaTable lua) {
		List<FPWireConnectionPoint> protoConnectionPoints = FPUtils.list(lua.get("connection_points"),
				FPWireConnectionPoint::new);

		consumer.accept(1, WirePoints.fromWireConnectionPoints(protoConnectionPoints, WireColor.RED, true));
		consumer.accept(2, WirePoints.fromWireConnectionPoints(protoConnectionPoints, WireColor.GREEN, true));
		consumer.accept(5, WirePoints.fromWireConnectionPoints(protoConnectionPoints, WireColor.COPPER, true));
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		protoPictures.getDefs(register);
	}

	@Override
	public void initFromPrototype() {
		// XXX strange that I have to force back_equals_front to be true
		protoPictures = new FPRotatedSprite(prototype.lua().get("pictures"), Optional.of(true));
	}

	@Override
	public double initWireConnector(Consumer<MapRenderable> register, MapEntity entity, List<MapEntity> wired) {
		MapPosition p1 = entity.getPosition();
		List<MapPosition> points = wired.stream().map(t -> {
			MapPosition p2 = t.getPosition();
			return p2.sub(p1);
		}).collect(Collectors.toList());
		double orientation = computePrincipalOrientation(points);

		protoPictures.defineSprites(entity.spriteRegister(register, Layer.HIGHER_OBJECT_ABOVE), orientation);

		return orientation;
	}
}
