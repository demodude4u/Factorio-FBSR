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

public class BurnerGeneratorRendering extends EntityRendererFactory {
	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		List<Sprite> sprites;
		LuaValue idleAnimation = prototype.lua().get("idle_animation");
		if (!idleAnimation.isnil()) {
			sprites = RenderUtils.getSpritesFromAnimation(idleAnimation, entity.getDirection());
		} else {
			sprites = RenderUtils.getSpritesFromAnimation(prototype.lua().get("animation"), entity.getDirection());
		}
		register.accept(RenderUtils.spriteRenderer(sprites, entity, prototype));
	}
}
