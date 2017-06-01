package com.demod.fbsr.render;

import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.WorldMap;

public class BeaconRendering extends TypeRendererFactory {
	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		Sprite baseSprite = getSpriteFromAnimation(prototype.lua().get("base_picture"));
		Sprite antennaSpriteShadow = getSpriteFromAnimation(prototype.lua().get("animation_shadow"));
		Sprite antennaSprite = getSpriteFromAnimation(prototype.lua().get("animation"));
		register.accept(spriteRenderer(baseSprite, entity, prototype));
		register.accept(spriteRenderer(antennaSpriteShadow, entity, prototype));
		register.accept(spriteRenderer(antennaSprite, entity, prototype));
	}
}
