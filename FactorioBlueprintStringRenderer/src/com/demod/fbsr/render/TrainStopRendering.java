package com.demod.fbsr.render;

import java.util.List;
import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.Renderer.Layer;

public class TrainStopRendering extends TypeRendererFactory {
	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		List<Sprite> railSprites = RenderUtils.getSpritesFromAnimation(prototype.lua().get("rail_overlay_animations"),
				entity.getDirection());
		List<Sprite> sprites = RenderUtils.getSpritesFromAnimation(prototype.lua().get("animations"), entity.getDirection());
		List<Sprite> topSprites = RenderUtils.getSpritesFromAnimation(prototype.lua().get("top_animations"), entity.getDirection());

		register.accept(RenderUtils.spriteRenderer(Layer.RAIL_BACKPLATES, railSprites, entity, prototype));
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY, sprites, entity, prototype));
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY2, topSprites, entity, prototype));
	}
}
