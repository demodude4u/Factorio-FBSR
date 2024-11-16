package com.demod.fbsr.entity;

import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.RenderUtils.SpriteDirDefList;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.WorldMap;

public class FluidTurretRendering extends EntityRendererFactory {

	private SpriteDirDefList protoBaseSprites;
	private SpriteDirDefList protoTurretSprites;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {

		register.accept(RenderUtils.spriteDirDefRenderer(protoBaseSprites, entity, protoSelectionBox));
		register.accept(RenderUtils.spriteDirDefRenderer(protoTurretSprites, entity, protoSelectionBox));
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		protoBaseSprites = RenderUtils
				.getDirSpritesFromAnimation(prototype.lua().get("graphics_set").get("base_visualisation"));
		protoTurretSprites = RenderUtils.getDirSpritesFromAnimation(prototype.lua().get("folded_animation"));
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity) {
		Direction dir = entity.getDirection();
		map.setPipe(dir.right().offset(dir.back().offset(entity.getPosition()), 0.5), dir.right());
		map.setPipe(dir.left().offset(dir.back().offset(entity.getPosition()), 0.5), dir.left());
	}
}
