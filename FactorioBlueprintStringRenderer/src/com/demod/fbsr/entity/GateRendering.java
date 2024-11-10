package com.demod.fbsr.entity;

import java.util.List;
import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.SpriteDef;
import com.demod.fbsr.WorldMap;

public class GateRendering extends EntityRendererFactory {

	private List<SpriteDef> protoVerticalSprites;
	private List<SpriteDef> protoHorizontalSprites;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {
		if (isVertical(entity)) {
			register.accept(RenderUtils.spriteDefRenderer(protoVerticalSprites, entity, protoSelectionBox));
		} else {
			register.accept(RenderUtils.spriteDefRenderer(protoHorizontalSprites, entity, protoSelectionBox));
		}
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		protoVerticalSprites = RenderUtils.getSpritesFromAnimation(prototype.lua().get("vertical_animation"));
		protoHorizontalSprites = RenderUtils.getSpritesFromAnimation(prototype.lua().get("horizontal_animation"));
	}

	private boolean isVertical(BlueprintEntity entity) {
		return entity.getDirection().cardinal() % 2 == 0;
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity) {
		if (isVertical(entity)) {
			map.setVerticalGate(entity.getPosition());
		} else {
			map.setHorizontalGate(entity.getPosition());
		}
	}
}
