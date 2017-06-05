package com.demod.fbsr.render;

import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Renderer.Layer;
import com.demod.fbsr.WorldMap;

public class ElectricPoleRendering extends TypeRendererFactory {

	// private static final int SpriteIndex = 2;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		Sprite sprite = RenderUtils.getSpriteFromAnimation(prototype.lua().get("pictures"));
		// sprite.source.x = sprite.source.width * SpriteIndex;
		sprite.source.x = sprite.source.width * entity.getDirection().cardinal();
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY3, sprite, entity, prototype));
	}
}
