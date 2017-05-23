package com.demod.fbsr.render;

import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.DataPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.WorldMap;

public class RailSignalRendering extends TypeRendererFactory {
	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			DataPrototype prototype) {
		Sprite railSprite = getSpriteFromAnimation(prototype.lua().get("rail_piece"));
		railSprite.source.x += railSprite.source.width * (entity.getDirection().ordinal());

		Sprite sprite = getSpriteFromAnimation(prototype.lua().get("animation"));
		sprite.source.y += sprite.source.height * (entity.getDirection().ordinal());

		register.accept(spriteRenderer(railSprite, entity, prototype));
		register.accept(spriteRenderer(sprite, entity, prototype));
	}
}
