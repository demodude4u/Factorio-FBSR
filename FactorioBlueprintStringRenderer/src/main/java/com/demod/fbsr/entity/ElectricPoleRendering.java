package com.demod.fbsr.entity;

import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Renderer.Layer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;

public class ElectricPoleRendering extends EntityRendererFactory {

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		Sprite spriteShadow = RenderUtils.getSpriteFromAnimation(prototype.lua().get("pictures").get("layers").get(2));
		spriteShadow.source.x = spriteShadow.source.width * entity.getDirection().cardinal();
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY3, spriteShadow, entity, prototype));

		Sprite sprite = RenderUtils.getSpriteFromAnimation(prototype.lua().get("pictures").get("layers").get(1));
		sprite.source.x = sprite.source.width * entity.getDirection().cardinal();
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY3, sprite, entity, prototype));
	}
}
