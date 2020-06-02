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

public class ArithmeticCombinatorRendering extends EntityRendererFactory {
	public static final Map<String, String> operationSprites = new HashMap<>();
	static {
		operationSprites.put("+", "plus_symbol_sprites");
		operationSprites.put("-", "minus_symbol_sprites");
		operationSprites.put("*", "multiply_symbol_sprites");
		operationSprites.put("/", "divide_symbol_sprites");
		operationSprites.put("AND", "and_symbol_sprites");
		operationSprites.put("OR", "or_symbol_sprites");
		operationSprites.put("XOR", "xor_symbol_sprites");
		operationSprites.put("MOD", "modulo_symbol_sprites");
		operationSprites.put("%", "modulo_symbol_sprites");
		operationSprites.put(">>", "right_shift_symbol_sprites");
		operationSprites.put("<<", "left_shift_symbol_sprites");
		operationSprites.put("^", "power_symbol_sprites");
	}

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		List<Sprite> sprites = RenderUtils.getSpritesFromAnimation(prototype.lua().get("sprites"),
				entity.getDirection());
		String operationString = entity.json().getJSONObject("control_behavior").getJSONObject("arithmetic_conditions")
				.getString("operation");
		List<Sprite> operatorSprite = RenderUtils.getSpritesFromAnimation(
				prototype.lua().get(operationSprites.get(operationString)), entity.getDirection());

		register.accept(RenderUtils.spriteRenderer(sprites, entity, prototype));
		register.accept(RenderUtils.spriteRenderer(operatorSprite, entity, prototype));
	}
}
