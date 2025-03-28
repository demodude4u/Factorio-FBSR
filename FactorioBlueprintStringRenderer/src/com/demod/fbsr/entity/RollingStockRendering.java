package com.demod.fbsr.entity;

import java.awt.Color;
import java.util.Optional;
import java.util.function.Consumer;

import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Layer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.entity.BSRollingStockEntity;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.SpriteDef;
import com.demod.fbsr.fp.FPColor;
import com.demod.fbsr.fp.FPRollingStockRotatedSlopedGraphics;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRect3D;
import com.demod.fbsr.map.MapRenderable;
import com.demod.fbsr.map.MapSprite;

public class RollingStockRendering extends EntityRendererFactory {

	private FPColor protoColor;
	private double protoJointDistance;
	private FPRollingStockRotatedSlopedGraphics protoPictures;
	private Optional<FPRollingStockRotatedSlopedGraphics> protoWheels;

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {

		// TODO sloped
		// TODO mask tinting with entity color

		BSRollingStockEntity bsEntity = entity.fromBlueprint();

		Color color;
		if (bsEntity.color.isPresent()) {
			color = bsEntity.color.get().createColor();
		} else {
			color = protoColor.createColor();
//			color = new FPColor(0.92, 0.07, 0, 0.5).createColor();
		}

		double orientation = entity.fromBlueprint().orientation.orElse(0);
		double orientation180 = orientation < 0.5 ? orientation + 0.5 : orientation - 0.5;
		double rotation = orientation * Math.PI * 2 + Math.PI * 0.5;
		double jointX = (protoJointDistance / 2.0) * Math.cos(rotation);
		double jointY = (protoJointDistance / 2.0) * Math.sin(rotation);
		double railShift = 0.25 * Math.abs(Math.cos(rotation));

		if (protoWheels.isPresent()) {
			MapPosition shiftPos1 = entity.getPosition().addUnit(-jointX, -jointY - railShift);
			MapPosition shiftPos2 = entity.getPosition().addUnit(jointX, jointY - railShift);

			protoWheels.get().rotated.defineSprites(s -> {
				register.accept(new MapSprite(s, Layer.HIGHER_OBJECT_UNDER, shiftPos1));
			}, orientation);
			protoWheels.get().rotated.defineSprites(s -> {
				register.accept(new MapSprite(s, Layer.HIGHER_OBJECT_UNDER, shiftPos2));
			}, orientation180);
		}

		Consumer<SpriteDef> spriteRegister = entity.spriteRegisterWithTintOverride(register, Layer.HIGHER_OBJECT_UNDER,
				color);
		protoPictures.rotated.defineSprites(spriteRegister, orientation);
	}

	@Override
	public MapRect3D getDrawBounds(MapEntity entity) {
		return drawBounds.rotateOrientation(entity.fromBlueprint().orientation.orElse(0), true)
				.shift(entity.getPosition());
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		protoPictures.rotated.getDefs(register);
		protoPictures.sloped.ifPresent(fp -> fp.getDefs(register));
		if (protoWheels.isPresent()) {
			protoWheels.get().rotated.getDefs(register);
			protoWheels.get().sloped.ifPresent(fp -> fp.getDefs(register));
		}
	}

	@Override
	public void initFromPrototype() {
		protoColor = new FPColor(prototype.lua().get("color"));
		protoJointDistance = prototype.lua().get("joint_distance").todouble();
		protoPictures = new FPRollingStockRotatedSlopedGraphics(prototype.lua().get("pictures"));
		protoWheels = FPUtils.opt(prototype.lua().get("wheels"), FPRollingStockRotatedSlopedGraphics::new);
	}

	@Override
	public Class<? extends BSEntity> getEntityClass() {
		return BSRollingStockEntity.class;
	}
}
