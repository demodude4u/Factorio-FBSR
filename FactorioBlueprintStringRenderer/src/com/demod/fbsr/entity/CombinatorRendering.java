package com.demod.fbsr.entity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.fp.FPSprite4Way;

public abstract class CombinatorRendering extends SimpleEntityRendering {

	private Map<String, FPSprite4Way> protoOperationSprites;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {
		super.createRenderers(register, map, dataTable, entity);

		Optional<String> operation = getOperation(entity);
		if (operation.isPresent()) {
			register.accept(RenderUtils.spriteRenderer(
					protoOperationSprites.get(operation.get()).createSprites(entity.getDirection()), entity,
					protoSelectionBox));
		}
	}

	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.sprite4Way(lua.get("sprites"));
	}

	public abstract void defineOperations(Map<String, String> operations);

	public abstract Optional<String> getOperation(BlueprintEntity entity);

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		Map<String, String> operations = new LinkedHashMap<>();
		defineOperations(operations);
		protoOperationSprites = new LinkedHashMap<>();
		for (Entry<String, String> entry : operations.entrySet()) {
			protoOperationSprites.put(entry.getKey(), new FPSprite4Way(prototype.lua().get(entry.getValue())));
		}
	}

}
