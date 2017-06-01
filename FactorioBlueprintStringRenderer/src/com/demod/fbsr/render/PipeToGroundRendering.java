package com.demod.fbsr.render;

import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.WorldMap;

public class PipeToGroundRendering extends TypeRendererFactory {

	public static String[] pipeToGroundCardinalNaming = { //
			"up", "right", "down", "left"//
	};

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		Sprite sprite = getSpriteFromAnimation(
				prototype.lua().get("pictures").get(pipeToGroundCardinalNaming[entity.getDirection().cardinal()]));
		register.accept(spriteRenderer(sprite, entity, prototype));
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, EntityPrototype prototype) {
		map.setPipe(entity.getPosition(), entity.getDirection());
	}

}
