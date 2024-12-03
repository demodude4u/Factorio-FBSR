package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.FBSR.EntityRenderingTuple;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Layer;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.WirePoints;
import com.demod.fbsr.WirePoints.WireColor;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.BSPosition;
import com.demod.fbsr.fp.FPRotatedSprite;
import com.demod.fbsr.fp.FPWireConnectionPoint;

public class ElectricPoleRendering extends EntityRendererFactory {

	private FPRotatedSprite protoPictures;

	@Override
	public void defineWirePoints(BiConsumer<Integer, WirePoints> consumer, LuaValue lua) {
		List<FPWireConnectionPoint> protoConnectionPoints = FPUtils.list(lua.get("connection_points"),
				FPWireConnectionPoint::new);

		consumer.accept(1, WirePoints.fromWireConnectionPoints(protoConnectionPoints, WireColor.RED, true));
		consumer.accept(2, WirePoints.fromWireConnectionPoints(protoConnectionPoints, WireColor.GREEN, true));
		consumer.accept(5, WirePoints.fromWireConnectionPoints(protoConnectionPoints, WireColor.COPPER, true));
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		// XXX strange that I have to force back_equals_front to be true
		protoPictures = new FPRotatedSprite(prototype.lua().get("pictures"), Optional.of(true));
	}

	@Override
	public double initWireConnector(Consumer<Renderer> register, BSEntity entity, List<EntityRenderingTuple> wired) {

		BSPosition pos = entity.position;

		Point2D.Double dirAvg = new Point2D.Double();
		for (EntityRenderingTuple tuple : wired) {
			BSPosition pos2 = tuple.entity.position;
			double dx = pos2.x - pos.x;
			double dy = pos2.y - pos.y;
			if (dx < 0) {
				dx *= -1;
				dy *= -1;
			}
			double dist = Math.sqrt(dx * dx + dy * dy);
			if (dist > 0) {
				dirAvg.x += dx / dist;
				dirAvg.y += dy / dist;
			}
		}
		double orientation;
		if (dirAvg.x == 0 || dirAvg.y == 0) {
			orientation = 0;
		} else {
			double rad = Math.atan2(dirAvg.y, dirAvg.x) + (Math.PI / 2.0);
			if (rad < 0) {
				rad += Math.PI * 2;
			}
			orientation = rad / (Math.PI * 2);
		}

		register.accept(RenderUtils.spriteRenderer(Layer.HIGHER_OBJECT_ABOVE, protoPictures.createSprites(orientation),
				entity, protoSelectionBox));

		return orientation;
	}
}
