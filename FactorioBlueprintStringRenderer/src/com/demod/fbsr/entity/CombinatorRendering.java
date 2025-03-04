package com.demod.fbsr.entity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.swing.Renderer;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.WirePoints;
import com.demod.fbsr.WirePoints.WireColor;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.fp.FPSprite4Way;
import com.demod.fbsr.fp.FPWireConnectionPoint;

public abstract class CombinatorRendering<E extends BSEntity> extends SimpleEntityRendering<E> {

	private Map<String, FPSprite4Way> protoOperationSprites;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, E entity) {
		super.createRenderers(register, map, entity);

		Optional<String> operation = getOperation(entity);
		if (operation.isPresent()) {
			register.accept(RenderUtils.spriteRenderer(
					protoOperationSprites.get(operation.get()).createSprites(data, entity.direction), entity,
					drawBounds));
		}
	}

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.sprite4Way(lua.get("sprites"));
	}

	public abstract void defineOperations(Map<String, String> operations);

	@Override
	public void defineWirePoints(BiConsumer<Integer, WirePoints> consumer, LuaTable lua) {
		List<FPWireConnectionPoint> protoInputConnectionPoints = FPUtils.list(lua.get("input_connection_points"),
				FPWireConnectionPoint::new);
		List<FPWireConnectionPoint> protoOutputConnectionPoints = FPUtils.list(lua.get("output_connection_points"),
				FPWireConnectionPoint::new);

		consumer.accept(1, WirePoints.fromWireConnectionPoints(protoInputConnectionPoints, WireColor.RED, false));
		consumer.accept(2, WirePoints.fromWireConnectionPoints(protoInputConnectionPoints, WireColor.GREEN, false));
		consumer.accept(3, WirePoints.fromWireConnectionPoints(protoOutputConnectionPoints, WireColor.RED, false));
		consumer.accept(4, WirePoints.fromWireConnectionPoints(protoOutputConnectionPoints, WireColor.GREEN, false));
	}

	public abstract Optional<String> getOperation(E entity);

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		Map<String, String> operations = new LinkedHashMap<>();
		defineOperations(operations);
		protoOperationSprites = new LinkedHashMap<>();
		for (Entry<String, String> entry : operations.entrySet()) {
			protoOperationSprites.put(entry.getKey(), new FPSprite4Way(prototype.lua().get(entry.getValue())));
		}
	}

}
