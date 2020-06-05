package com.demod.fbsr.entity;

import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;

public class PipeToGroundRendering extends EntityRendererFactory {

	public static String[] pipeToGroundCardinalNaming = { //
			"up", "right", "down", "left"//
	};

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		Sprite sprite = RenderUtils.getSpriteFromAnimation(
				prototype.lua().get("pictures").get(pipeToGroundCardinalNaming[entity.getDirection().cardinal()]));
		register.accept(RenderUtils.spriteRenderer(sprite, entity, prototype));
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, EntityPrototype prototype) {
		map.setPipe(entity.getPosition(), entity.getDirection());
	}

}
