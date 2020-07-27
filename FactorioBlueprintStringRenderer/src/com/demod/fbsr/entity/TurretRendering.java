package com.demod.fbsr.entity;

import java.util.List;
import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;

public class TurretRendering extends EntityRendererFactory {
	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		List<Sprite> baseSprites = RenderUtils.getSpritesFromAnimation(prototype.lua().get("base_picture"));
		register.accept(RenderUtils.spriteRenderer(baseSprites, entity, prototype));

		List<Sprite> turretSprites = RenderUtils.getSpritesFromAnimation(prototype.lua().get("folded_animation"));
		turretSprites.forEach(s -> s.source.y = s.source.height * entity.getDirection().cardinal());
		register.accept(RenderUtils.spriteRenderer(turretSprites, entity, prototype));
	}

}
