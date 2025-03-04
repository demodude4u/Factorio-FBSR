package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.Renderer;

import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.Layer;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.entity.BSRailSignalBaseEntity;
import com.demod.fbsr.fp.FPBoundingBox;
import com.demod.fbsr.fp.FPRailSignalPictureSet;
import com.demod.fbsr.fp.FPVector;
import com.demod.fbsr.map.MapRect3D;

public abstract class RailSignalBaseRendering extends EntityRendererFactory<BSRailSignalBaseEntity> {

	protected FPRailSignalPictureSet groundPictureSet;
	protected FPRailSignalPictureSet elevatedPictureSet;

	// TODO circuit connectors

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, BSRailSignalBaseEntity entity) {

		boolean elevated = entity.railLayer.filter(s -> s.equals("elevated")).isPresent();
		FPRailSignalPictureSet pictureSet = elevated ? elevatedPictureSet : groundPictureSet;
		Layer layer = elevated ? Layer.ELEVATED_RAIL_OBJECT : Layer.OBJECT;

		// 16 directions
		int direction = entity.directionRaw;

		int align = direction * 12;
		FPVector shift = pictureSet.selectionBoxShift.get(align);
		FPBoundingBox shiftedSelectionBox = protoSelectionBox.shift(shift);

		int railPieceFrame = pictureSet.railPiece.alignToFrameIndex.get(align);
		List<Sprite> railPieceSprites = pictureSet.railPiece.sprites.createSprites(data, railPieceFrame);

		int structureIndex = pictureSet.structureAlignToAnimationIndex.get(align);
		List<Sprite> structureSprites = pictureSet.structure.createSprites(data, structureIndex, 0);

		if (elevated) {
			Point2D.Double elevatedShift = new Point2D.Double(0, -3);
			RenderUtils.shiftSprites(railPieceSprites, elevatedShift);
			RenderUtils.shiftSprites(structureSprites, elevatedShift);
		}

		MapRect3D drawBounds = new MapRect3D(shiftedSelectionBox, this.drawBounds.heightfp);
		register.accept(RenderUtils.spriteRenderer(layer, railPieceSprites, entity, drawBounds));
		register.accept(RenderUtils.spriteRenderer(layer, structureSprites, entity, drawBounds));
	}

	@Override
	public void initFromPrototype() {
		groundPictureSet = new FPRailSignalPictureSet(prototype.lua().get("ground_picture_set"));
		elevatedPictureSet = new FPRailSignalPictureSet(prototype.lua().get("elevated_picture_set"));
	}

}
