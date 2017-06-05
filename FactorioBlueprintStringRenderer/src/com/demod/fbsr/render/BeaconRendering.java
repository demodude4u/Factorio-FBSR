package com.demod.fbsr.render;

import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.WorldMap;

public class BeaconRendering extends TypeRendererFactory {
	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		Sprite baseSprite = RenderUtils.getSpriteFromAnimation(prototype.lua().get("base_picture"));
		Sprite antennaSpriteShadow = RenderUtils.getSpriteFromAnimation(prototype.lua().get("animation_shadow"));
		Sprite antennaSprite = RenderUtils.getSpriteFromAnimation(prototype.lua().get("animation"));
		register.accept(RenderUtils.spriteRenderer(baseSprite, entity, prototype));
		register.accept(RenderUtils.spriteRenderer(antennaSpriteShadow, entity, prototype));
		register.accept(RenderUtils.spriteRenderer(antennaSprite, entity, prototype));
	}
}
