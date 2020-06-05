package com.demod.fbsr.entity;

import java.util.List;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;

public class OffshorePumpRendering extends EntityRendererFactory {

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		List<Sprite> sprites;
		LuaValue graphicsSet = prototype.lua().get("graphics_set");
		if (!graphicsSet.isnil()) {
			sprites = RenderUtils.getSpritesFromAnimation(graphicsSet.get("animation"), entity.getDirection());
		} else {
			sprites = RenderUtils.getSpritesFromAnimation(prototype.lua().get("picture"), entity.getDirection());
		}
		register.accept(RenderUtils.spriteRenderer(sprites, entity, prototype));
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, EntityPrototype prototype) {
		map.setPipe(entity.getPosition(), entity.getDirection().back());
	}
}
