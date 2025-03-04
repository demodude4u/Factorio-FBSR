package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.swing.Renderer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Layer;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.fp.FPRotatedSprite;

public class RollingStockRendering extends EntityRendererFactory<BSEntity> {

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
	private Optional<FPRollingStockRotatedSlopedGraphics> protoWheels;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, BSEntity entity) {

		// TODO sloped
		// TODO mask tinting with entity color

		double orientation = entity.orientation.getAsDouble();
		double orientation180 = orientation < 0.5 ? orientation + 0.5 : orientation - 0.5;
		double rotation = orientation * Math.PI * 2 + Math.PI * 0.5;
		double jointX = (protoJointDistance / 2.0) * Math.cos(rotation);
		double jointY = (protoJointDistance / 2.0) * Math.sin(rotation);
		double railShift = 0.25 * Math.abs(Math.cos(rotation));

		if (protoWheels.isPresent()) {
			List<Sprite> wheelSprites1 = protoWheels.get().rotated.createSprites(data, orientation);
			List<Sprite> wheelSprites2 = protoWheels.get().rotated.createSprites(data, orientation180);

			RenderUtils.shiftSprites(wheelSprites1, new Point2D.Double(-jointX, -jointY - railShift));
			RenderUtils.shiftSprites(wheelSprites2, new Point2D.Double(jointX, jointY - railShift));

			register.accept(
					RenderUtils.spriteRenderer(Layer.HIGHER_OBJECT_UNDER, wheelSprites1, entity, drawBounds));
			register.accept(
					RenderUtils.spriteRenderer(Layer.HIGHER_OBJECT_UNDER, wheelSprites2, entity, drawBounds));
		}

		register.accept(RenderUtils.spriteRenderer(Layer.HIGHER_OBJECT_UNDER,
				protoPictures.rotated.createSprites(data, orientation), entity, drawBounds));
	}

	@Override
	public void initFromPrototype() {

		protoJointDistance = prototype.lua().get("joint_distance").todouble();
		protoPictures = new FPRollingStockRotatedSlopedGraphics(prototype.lua().get("pictures"));
		protoWheels = FPUtils.opt(prototype.lua().get("wheels"), FPRollingStockRotatedSlopedGraphics::new);
	}
}
