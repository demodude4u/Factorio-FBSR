package com.demod.fbsr.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.fp.FPSprite4Way;

public class ArithmeticCombinatorRendering extends SimpleSprite4WayRendering {
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

	private HashMap<String, FPSprite4Way> protoOperationSprites;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {
		super.createRenderers(register, map, dataTable, entity);

		String operationString = entity.json().getJSONObject("control_behavior").getJSONObject("arithmetic_conditions")
				.getString("operation");
		register.accept(RenderUtils.spriteRenderer(
				protoOperationSprites.get(operationString).createSprites(entity.getDirection()), entity,
				protoSelectionBox));
	}

	@Override
	public FPSprite4Way getSprite(LuaValue lua) {
		return new FPSprite4Way(lua.get("sprites"));
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		protoOperationSprites = new HashMap<>();
		for (Entry<String, String> entry : operationSprites.entrySet()) {
			protoOperationSprites.put(entry.getKey(), new FPSprite4Way(prototype.lua().get(entry.getValue())));
		}
	}
}
