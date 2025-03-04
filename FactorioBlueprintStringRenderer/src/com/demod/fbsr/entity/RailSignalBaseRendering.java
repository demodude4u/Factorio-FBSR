package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.swing.Renderer;

import org.json.JSONObject;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.BSUtils;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Layer;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.entity.RailSignalBaseRendering.BSRailSignalBaseEntity;
import com.demod.fbsr.fp.FPAnimation;
import com.demod.fbsr.fp.FPBoundingBox;
import com.demod.fbsr.fp.FPRotatedAnimation;
import com.demod.fbsr.fp.FPVector;
import com.demod.fbsr.legacy.LegacyBlueprintEntity;
import com.demod.fbsr.map.MapRect3D;

public abstract class RailSignalBaseRendering extends EntityRendererFactory<BSRailSignalBaseEntity> {

	public static class BSRailSignalBaseEntity extends BSEntity {
		public final Optional<String> railLayer;

		public BSRailSignalBaseEntity(JSONObject json) {
			super(json);

			railLayer = BSUtils.optString(json, "rail_layer");
		}

		public BSRailSignalBaseEntity(LegacyBlueprintEntity legacy) {
			super(legacy);

			railLayer = Optional.empty();
		}
	}

	public static class FPRailSignalPictureSet {
		public final FPRotatedAnimation structure;
		public final FPRailSignalStaticSpriteLayer railPiece;
		public final List<Integer> structureAlignToAnimationIndex;
		public final List<FPVector> selectionBoxShift;

		public FPRailSignalPictureSet(LuaValue lua) {
			structure = new FPRotatedAnimation(lua.get("structure"));
			railPiece = new FPRailSignalStaticSpriteLayer(lua.get("rail_piece"));
			structureAlignToAnimationIndex = FPUtils.list(lua.get("structure_align_to_animation_index"),
					LuaValue::toint);
			selectionBoxShift = FPUtils.list(lua.get("selection_box_shift"), FPVector::new);
		}
	}

	public static class FPRailSignalStaticSpriteLayer {
		public final FPAnimation sprites;
		public final List<Integer> alignToFrameIndex;

		public FPRailSignalStaticSpriteLayer(LuaValue lua) {
			sprites = new FPAnimation(lua.get("sprites"));
			alignToFrameIndex = FPUtils.list(lua.get("align_to_frame_index"), LuaValue::toint);
		}
	}

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

		MapRect3D drawBounds = new MapRect3D(shiftedSelectionBox, this.drawBounds.height);
		register.accept(RenderUtils.spriteRenderer(layer, railPieceSprites, entity, drawBounds));
		register.accept(RenderUtils.spriteRenderer(layer, structureSprites, entity, drawBounds));
	}

	@Override
	public void initFromPrototype() {
		groundPictureSet = new FPRailSignalPictureSet(prototype.lua().get("ground_picture_set"));
		elevatedPictureSet = new FPRailSignalPictureSet(prototype.lua().get("elevated_picture_set"));
	}

}
