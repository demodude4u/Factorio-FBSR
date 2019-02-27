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

public class AccumulatorRendering extends EntityRendererFactory {
	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		Sprite spriteShadow = RenderUtils.getSpriteFromAnimation(prototype.lua().get("picture").get("layers").get(2));
		register.accept(RenderUtils.spriteRenderer(spriteShadow, entity, prototype));

		Sprite sprite = RenderUtils.getSpriteFromAnimation(prototype.lua().get("picture").get("layers").get(1));
		register.accept(RenderUtils.spriteRenderer(sprite, entity, prototype));
	}
}
