package com.demod.fbsr.entity;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.SpriteDef;
import com.demod.fbsr.WorldMap;

public class TurretRendering extends EntityRendererFactory {

	private List<SpriteDef> protoBaseSprites;
	private List<SpriteDef> protoTurretSprites;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {

		register.accept(RenderUtils.spriteDefRenderer(protoBaseSprites, entity, protoSelectionBox));

		List<Sprite> turretSprites = protoTurretSprites.stream().map(SpriteDef::createSprite)
				.collect(Collectors.toList());
		turretSprites.forEach(s -> s.source.y = s.source.height * entity.getDirection().cardinal());
		register.accept(RenderUtils.spriteRenderer(turretSprites, entity, protoSelectionBox));
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		protoBaseSprites = RenderUtils.getSpritesFromAnimation(prototype.lua().get("base_picture"));
		protoTurretSprites = RenderUtils.getSpritesFromAnimation(prototype.lua().get("folded_animation"));
	}

}
