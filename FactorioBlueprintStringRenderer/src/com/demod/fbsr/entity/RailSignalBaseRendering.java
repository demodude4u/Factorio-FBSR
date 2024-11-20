package com.demod.fbsr.entity;

import java.util.List;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.fp.FPAnimation;
import com.demod.fbsr.fp.FPBoundingBox;
import com.demod.fbsr.fp.FPRotatedAnimation;
import com.demod.fbsr.fp.FPVector;

public abstract class RailSignalBaseRendering extends EntityRendererFactory {

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

	// TODO elevated versions

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {
		// TODO verify assumptions made with 192 rotations

		// 16 directions
		int direction = entity.json().optInt("direction", 0);

		// TODO there is no orientation value, so how do I go from 16 directions to 192?
		int align = direction * 12; // crude convert dir 16 to 192...

		FPVector shift = groundPictureSet.selectionBoxShift.get(align);
		FPBoundingBox shiftedSelectionBox = protoSelectionBox.shift(shift);

		int railPieceFrame = groundPictureSet.railPiece.alignToFrameIndex.get(align);
		register.accept(RenderUtils.spriteRenderer(groundPictureSet.railPiece.sprites.createSprites(railPieceFrame),
				entity, shiftedSelectionBox));

		int structureIndex = groundPictureSet.structureAlignToAnimationIndex.get(align);
		register.accept(RenderUtils.spriteRenderer(groundPictureSet.structure.createSprites(structureIndex, 0), entity,
				shiftedSelectionBox));
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		groundPictureSet = new FPRailSignalPictureSet(prototype.lua().get("ground_picture_set"));
		elevatedPictureSet = new FPRailSignalPictureSet(prototype.lua().get("elevated_picture_set"));
	}

}
