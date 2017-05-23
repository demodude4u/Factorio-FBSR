package com.demod.fbsr.render;

import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.DataPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.render.Renderer.Layer;

public class ElectricPoleRendering extends TypeRendererFactory {

	private static final int SpriteIndex = 2;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			DataPrototype prototype) {
		Sprite sprite = getSpriteFromAnimation(prototype.lua().get("pictures"));
		sprite.source.x = sprite.source.width * SpriteIndex;
		register.accept(spriteRenderer(Layer.ENTITY3, sprite, entity, prototype));
	}
}
