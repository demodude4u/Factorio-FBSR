package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.Renderer;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.FBSR.EntityRenderingTuple;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Layer;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.WirePoints;
import com.demod.fbsr.WirePoints.WireColor;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.BSPosition;
import com.demod.fbsr.fp.FPRotatedSprite;
import com.demod.fbsr.fp.FPWireConnectionPoint;

public class ElectricPoleRendering extends EntityRendererFactory<BSEntity> {

	private FPRotatedSprite protoPictures;

	public double computePrincipalOrientation(List<Point2D.Double> points) {
		if (points.isEmpty()) {
			return 0;
		}

		if (points.size() == 1) {
			Point2D.Double p = points.get(0);
			double rad = Math.atan2(p.y, p.x) + (Math.PI / 2.0);
			if (rad < 0) {
				rad += Math.PI * 2;
			}
			return rad / (Math.PI * 2);
		}

		double meanX = 0, meanY = 0;
		for (Point2D.Double p : points) {
			meanX += p.x;
			meanY += p.y;
		}
		meanX /= points.size();
		meanY /= points.size();

		double Sxx = 0, Syy = 0, Sxy = 0;
		for (Point2D.Double p : points) {
			double dx = p.x - meanX;
			double dy = p.y - meanY;
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
	public void createRenderers(Consumer<Renderer> register, WorldMap map, BSEntity entity) {
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
	public void initFromPrototype() {
		// XXX strange that I have to force back_equals_front to be true
		protoPictures = new FPRotatedSprite(prototype.lua().get("pictures"), Optional.of(true));
	}

	@Override
	public double initWireConnector(Consumer<Renderer> register, BSEntity entity, List<EntityRenderingTuple> wired) {
		BSPosition p1 = entity.position;
		List<Point2D.Double> points = wired.stream().map(t -> {
			BSPosition p2 = t.entity.position;
			return new Point2D.Double(p2.x - p1.x, p2.y - p1.y);
		}).collect(Collectors.toList());
		double orientation = computePrincipalOrientation(points);

		register.accept(RenderUtils.spriteRenderer(Layer.HIGHER_OBJECT_ABOVE,
				protoPictures.createSprites(data, orientation), entity, drawBounds));

		return orientation;
	}
}
