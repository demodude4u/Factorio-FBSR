package com.demod.fbsr.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;

public class DeciderCombinatorRendering extends EntityRendererFactory {
	public static final Map<String, String> operationSprites = new HashMap<>();
	static {
		operationSprites.put("=", "equal_symbol_sprites");
		operationSprites.put(">", "greater_symbol_sprites");
		operationSprites.put("<", "less_symbol_sprites");
		operationSprites.put("\u2260", "not_equal_symbol_sprites");
		operationSprites.put("\u2264", "less_or_equal_symbol_sprites");
		operationSprites.put("\u2265", "greater_or_equal_symbol_sprites");
	}

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		List<Sprite> sprites = RenderUtils.getSpritesFromAnimation(
				prototype.lua().get("sprites").get(entity.getDirection().name().toLowerCase()));
		Sprite operatorSprite = RenderUtils.getSpriteFromAnimation(prototype.lua()
				.get(operationSprites.get(entity.json().getJSONObject("control_behavior")
						.getJSONObject("decider_conditions").getString("comparator")))
				.get(entity.getDirection().name().toLowerCase()));

		register.accept(RenderUtils.spriteRenderer(sprites, entity, prototype));
		register.accept(RenderUtils.spriteRenderer(operatorSprite, entity, prototype));
	}
}
