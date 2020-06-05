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
import com.demod.fbsr.Renderer.Layer;

public class RocketSiloRendering extends EntityRendererFactory {

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		Sprite baseSprite = RenderUtils.getSpriteFromAnimation(prototype.lua().get("base_day_sprite"));
		Sprite shadowSprite = RenderUtils.getSpriteFromAnimation(prototype.lua().get("shadow_sprite"));
		Sprite doorFrontSprite = RenderUtils.getSpriteFromAnimation(prototype.lua().get("door_front_sprite"));
		Sprite doorBackSprite = RenderUtils.getSpriteFromAnimation(prototype.lua().get("door_back_sprite"));

		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY, shadowSprite, entity, prototype));
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY, doorFrontSprite, entity, prototype));
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY, doorBackSprite, entity, prototype));
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY2, baseSprite, entity, prototype));
	}
}
