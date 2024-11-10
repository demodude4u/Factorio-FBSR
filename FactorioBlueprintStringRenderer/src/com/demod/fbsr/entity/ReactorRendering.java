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

public class ReactorRendering extends EntityRendererFactory {

	private SpriteDirDefList protoDirSprites2;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {
		register.accept(RenderUtils.spriteDirDefRenderer(protoDirSprites, entity, protoSelectionBox));
		register.accept(RenderUtils.spriteDirDefRenderer(protoDirSprites2, entity, protoSelectionBox));
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		protoDirSprites = RenderUtils.getDirSpritesFromAnimation(prototype.lua().get("picture"));
		protoDirSprites2 = RenderUtils.getDirSpritesFromAnimation(prototype.lua().get("lower_layer_picture"));
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity) {
		for (Direction dir : Direction.values()) {
			map.setHeatPipe(dir.offset(entity.getPosition(), 2));
		}
	}
}
