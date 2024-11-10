package com.demod.fbsr.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.RenderUtils.SpriteDirDefList;
import com.demod.fbsr.Renderer;
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

	private HashMap<String, SpriteDirDefList> protoOperationSprites;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {
		register.accept(RenderUtils.spriteDirDefRenderer(protoDirSprites, entity, protoSelectionBox));

		if (!entity.isJsonNewFormat()) {
			String comparatorString = entity.json().getJSONObject("control_behavior")
					.getJSONObject("decider_conditions").getString("comparator");
			register.accept(RenderUtils.spriteDirDefRenderer(protoOperationSprites.get(comparatorString), entity,
					protoSelectionBox));
		}
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		protoDirSprites = RenderUtils.getDirSpritesFromAnimation(prototype.lua().get("sprites"));
		protoOperationSprites = new HashMap<>();
		for (Entry<String, String> entry : operationSprites.entrySet()) {
			protoOperationSprites.put(entry.getKey(),
					RenderUtils.getDirSpritesFromAnimation(prototype.lua().get(entry.getValue())));
		}
	}
}
