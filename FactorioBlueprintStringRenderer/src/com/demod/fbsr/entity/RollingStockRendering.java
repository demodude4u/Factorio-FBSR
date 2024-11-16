package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Renderer.Layer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.fp.FPRotatedSprite;

public class RollingStockRendering extends EntityRendererFactory {

	public static class FPRollingStockRotatedSlopedGraphics {
		public final FPRotatedSprite rotated;
		public final double slopeAngleBetweenFrames;
		public final boolean slopeBackEqualsFront;
		public final FPRotatedSprite sloped;

		public FPRollingStockRotatedSlopedGraphics(LuaValue lua) {
			rotated = new FPRotatedSprite(lua.get("rotated"));
			slopeAngleBetweenFrames = lua.get("slope_angle_between_frames").optdouble(1.333);
			slopeBackEqualsFront = lua.get("slope_back_equals_front").optboolean(false);
			sloped = new FPRotatedSprite(lua.get("sloped"));
		}
	}

	private double protoJointDistance;
	private FPRollingStockRotatedSlopedGraphics protoPictures;
	private FPRollingStockRotatedSlopedGraphics protoWheels;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {

		// TODO sloped
		// TODO mask tinting with entity color

		double orientation = entity.json().getDouble("orientation");
		double orientation180 = orientation < 0.5 ? orientation + 0.5 : orientation - 0.5;
		double rotation = orientation * Math.PI * 2 + Math.PI * 0.5;
		double jointX = (protoJointDistance / 2.0) * Math.cos(rotation);
		double jointY = (protoJointDistance / 2.0) * Math.sin(rotation);
		double railShift = 0.25 * Math.abs(Math.cos(rotation));

		List<Sprite> wheelSprites1 = protoWheels.rotated.createSprites(orientation);
		List<Sprite> wheelSprites2 = protoWheels.rotated.createSprites(orientation180);

		RenderUtils.shiftSprites(wheelSprites1, new Point2D.Double(-jointX, -jointY - railShift));
		RenderUtils.shiftSprites(wheelSprites2, new Point2D.Double(jointX, jointY - railShift));

		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY2, wheelSprites1, entity, protoSelectionBox));
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY2, wheelSprites2, entity, protoSelectionBox));
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY2, protoPictures.rotated.createSprites(orientation),
				entity, protoSelectionBox));
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {

		protoJointDistance = prototype.lua().get("joint_distance").todouble();
		protoPictures = new FPRollingStockRotatedSlopedGraphics(prototype.lua().get("pictures"));
		protoWheels = new FPRollingStockRotatedSlopedGraphics(prototype.lua().get("wheels"));
	}
}
