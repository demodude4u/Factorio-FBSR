package com.demod.fbsr.entity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Layer;
import com.demod.fbsr.WirePoint;
import com.demod.fbsr.WirePoint.WireColor;
import com.demod.fbsr.bind.Bindings;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.SpriteDef;
import com.demod.fbsr.fp.FPSprite4Way;
import com.demod.fbsr.fp.FPWireConnectionPoint;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapRenderable;

public abstract class CombinatorRendering extends EntityWithOwnerRendering {

	private Map<String, FPSprite4Way> protoOperationSprites;
	private List<FPWireConnectionPoint> protoInputConnectionPoints;
	private List<FPWireConnectionPoint> protoOutputConnectionPoints;

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		Optional<String> operation = getOperation(entity);
		if (operation.isPresent()) {
			Consumer<SpriteDef> spriteRegister = entity.spriteRegister(register, Layer.OBJECT);

			FPSprite4Way fp = protoOperationSprites.get(operation.get());
			if (fp != null) {
				fp.defineSprites(spriteRegister, entity.getDirection());
			}
		}
	}

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);

		bind.sprite4Way(lua.get("sprites"));
	}

	public abstract void defineOperations(Map<String, String> operations);

	@Override
	public void createWireConnector(Consumer<MapRenderable> register, BiConsumer<Integer, WirePoint> registerWirePoint,
			MapEntity entity, List<MapEntity> wired, WorldMap map) {
		super.createWireConnector(register, registerWirePoint, entity, wired, map);

		if (wired.isEmpty()) {
			return;
		}

		int index = entity.getDirection().cardinal();
		FPWireConnectionPoint input = protoInputConnectionPoints.get(index);
		FPWireConnectionPoint output = protoOutputConnectionPoints.get(index);
		registerWirePoint.accept(1, WirePoint.fromConnectionPoint(WireColor.RED, input, entity));
		registerWirePoint.accept(2, WirePoint.fromConnectionPoint(WireColor.GREEN, input, entity));
		registerWirePoint.accept(3, WirePoint.fromConnectionPoint(WireColor.RED, output, entity));
		registerWirePoint.accept(4, WirePoint.fromConnectionPoint(WireColor.GREEN, output, entity));
	}

	public abstract Optional<String> getOperation(MapEntity entity);

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		super.initAtlas(register);

		protoOperationSprites.values().forEach(fp -> fp.getDefs(register));
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		Map<String, String> operations = new LinkedHashMap<>();
		defineOperations(operations);
		protoOperationSprites = new LinkedHashMap<>();
		for (Entry<String, String> entry : operations.entrySet()) {
			LuaValue luaOperation = prototype.lua().get(entry.getValue());
			if (!luaOperation.isnil()) {
				protoOperationSprites.put(entry.getKey(), new FPSprite4Way(profile, luaOperation));
			}
		}

		protoInputConnectionPoints = FPUtils.list(prototype.lua().get("input_connection_points"),
				FPWireConnectionPoint::new);
		protoOutputConnectionPoints = FPUtils.list(prototype.lua().get("output_connection_points"),
				FPWireConnectionPoint::new);
	}

}
