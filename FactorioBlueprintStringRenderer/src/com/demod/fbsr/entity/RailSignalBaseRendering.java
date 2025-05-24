package com.demod.fbsr.entity;

import java.util.function.Consumer;

import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.Layer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.entity.BSRailSignalBaseEntity;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.SpriteDef;
import com.demod.fbsr.fp.FPRailSignalPictureSet;
import com.demod.fbsr.fp.FPVector;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapRect3D;
import com.demod.fbsr.map.MapRenderable;
import com.demod.fbsr.map.MapSprite;
import com.google.common.collect.ImmutableList;

public abstract class RailSignalBaseRendering extends EntityWithOwnerRendering {
	private static final int FRAME = 0;

	protected FPRailSignalPictureSet groundPictureSet;
	protected FPRailSignalPictureSet elevatedPictureSet;

	// TODO circuit connectors

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		BSRailSignalBaseEntity bsEntity = entity.<BSRailSignalBaseEntity>fromBlueprint();

		boolean elevated = bsEntity.railLayer.filter(s -> s.equals("elevated")).isPresent();
		FPRailSignalPictureSet pictureSet = elevated ? elevatedPictureSet : groundPictureSet;

		Consumer<SpriteDef> shiftedRegister;
		if (elevated) {
			shiftedRegister = s -> register
					.accept(new MapSprite(s, Layer.ELEVATED_RAIL_OBJECT, entity.getPosition().addUnit(0, -3)));
		} else {
			shiftedRegister = s -> register.accept(new MapSprite(s, Layer.OBJECT, entity.getPosition()));
		}

		int direction = entity.fromBlueprint().directionRaw;
		int align = direction * 12;

		int railPieceFrame = pictureSet.railPiece.alignToFrameIndex.get(align);
		pictureSet.railPiece.sprites.defineSprites(shiftedRegister, railPieceFrame);

		int structureIndex = pictureSet.structureAlignToAnimationIndex.get(align);
		pictureSet.structure.defineSprites(shiftedRegister, structureIndex, FRAME);
	}

	@Override
	public MapRect3D getDrawBounds(MapEntity entity) {
		MapRect3D ret = super.getDrawBounds(entity);

		BSRailSignalBaseEntity bsEntity = entity.<BSRailSignalBaseEntity>fromBlueprint();
		boolean elevated = bsEntity.railLayer.filter(s -> s.equals("elevated")).isPresent();
		FPRailSignalPictureSet pictureSet = elevated ? elevatedPictureSet : groundPictureSet;

		// 16 directions
		int direction = entity.fromBlueprint().directionRaw;
		int align = direction * 12;
		FPVector shift = pictureSet.selectionBoxShift.get(align);
		ret = ret.shiftUnit(shift.x, shift.y);

		if (elevated) {
			ret = ret.shiftHeightUnit(3);
		}

		return ret;
	}

	@Override
	public Class<? extends BSEntity> getEntityClass() {
		return BSRailSignalBaseEntity.class;
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		super.initAtlas(register);

		for (FPRailSignalPictureSet pictureSet : ImmutableList.of(groundPictureSet, elevatedPictureSet)) {
			for (int direction = 0; direction < 16; direction++) {
				int align = direction * 12;

				int railPieceFrame = pictureSet.railPiece.alignToFrameIndex.get(align);
				pictureSet.railPiece.sprites.defineSprites(register, railPieceFrame);

				int structureIndex = pictureSet.structureAlignToAnimationIndex.get(align);
				pictureSet.structure.defineSprites(register, structureIndex, FRAME);
			}
		}
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();
		
		groundPictureSet = new FPRailSignalPictureSet(profile, prototype.lua().get("ground_picture_set"));
		elevatedPictureSet = new FPRailSignalPictureSet(profile, prototype.lua().get("elevated_picture_set"));
	}
}
