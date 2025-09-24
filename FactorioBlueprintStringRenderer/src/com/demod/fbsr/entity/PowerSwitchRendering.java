package com.demod.fbsr.entity;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.WirePoint;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.WirePoint.WireColor;
import com.demod.fbsr.bind.Bindings;
import com.demod.fbsr.fp.FPWireConnectionPoint;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapRenderable;
import com.google.common.collect.ImmutableList;

@EntityType("power-switch")
public class PowerSwitchRendering extends EntityWithOwnerRendering {

	private FPWireConnectionPoint protoCircuitPoint;
	private FPWireConnectionPoint protoLeftPoint;
	private FPWireConnectionPoint protoRightPoint;

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);

		bind.animation(lua.get("power_on_animation"));
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		protoCircuitPoint = new FPWireConnectionPoint(prototype.lua().get("circuit_wire_connection_point"));
		protoLeftPoint = new FPWireConnectionPoint(prototype.lua().get("left_wire_connection_point"));
		protoRightPoint = new FPWireConnectionPoint(prototype.lua().get("right_wire_connection_point"));
	}

	@Override
	public void createWireConnector(Consumer<MapRenderable> register, BiConsumer<Integer, WirePoint> registerWirePoint,
			MapEntity entity, List<MapEntity> wired, WorldMap map) {
		super.createWireConnector(register, registerWirePoint, entity, wired, map);

		if (wired.isEmpty()) {
			return;
		}

		registerWirePoint.accept(1, WirePoint.fromConnectionPoint(WireColor.RED, protoCircuitPoint, entity));
		registerWirePoint.accept(2, WirePoint.fromConnectionPoint(WireColor.GREEN, protoCircuitPoint, entity));
		registerWirePoint.accept(5, WirePoint.fromConnectionPoint(WireColor.COPPER, protoLeftPoint, entity));
		registerWirePoint.accept(6, WirePoint.fromConnectionPoint(WireColor.COPPER, protoRightPoint, entity));
	}
}
