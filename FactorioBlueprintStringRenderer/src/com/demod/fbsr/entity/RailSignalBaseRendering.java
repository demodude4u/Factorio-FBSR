package com.demod.fbsr.entity;

import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.fp.FPAnimation;
import com.demod.fbsr.fp.FPRotatedAnimation;

public abstract class RailSignalBaseRendering extends EntityRendererFactory {

	protected FPAnimation protoGroundRailPieceSprites;
	protected FPRotatedAnimation protoGroundStructure;

	// TODO elevated versions

	// Proto Oddities (rail-signal):
	// - selection_box_shift (192)
	// - structure_align_to_animation_index (192)
	// - rail_piece.align_to_frame_index (192)
	// - rail_piece.sprites (50 frames)

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {
		// TODO
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		LuaValue luaGround = prototype.lua().get("ground_picture_set");
		LuaValue luaGroundRailPiece = luaGround.get("rail_piece");
		protoGroundRailPieceSprites = new FPAnimation(luaGroundRailPiece.get("sprites"));
		protoGroundStructure = new FPRotatedAnimation(luaGround.get("structure"));
	}

}
