package com.demod.fbsr.render;

import java.util.List;
import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.DataPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.render.Renderer.Layer;

public class TrainStopRendering extends TypeRendererFactory {
	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			DataPrototype prototype) {
		List<Sprite> railSprites = getSpritesFromAnimation(prototype.lua().get("rail_overlay_animations"),
				entity.getDirection());
		List<Sprite> sprites = getSpritesFromAnimation(prototype.lua().get("animations"), entity.getDirection());
		List<Sprite> topSprites = getSpritesFromAnimation(prototype.lua().get("top_animations"), entity.getDirection());

		register.accept(spriteRenderer(Layer.RAIL_BACKPLATES, railSprites, entity, prototype));
		register.accept(spriteRenderer(Layer.ENTITY, sprites, entity, prototype));
		register.accept(spriteRenderer(Layer.ENTITY2, topSprites, entity, prototype));
	}
}
