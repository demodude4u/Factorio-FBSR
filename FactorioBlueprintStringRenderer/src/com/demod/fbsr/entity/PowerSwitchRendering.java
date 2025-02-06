package com.demod.fbsr.entity;

import java.util.function.BiConsumer;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.WirePoints;
import com.demod.fbsr.WirePoints.WireColor;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.fp.FPWireConnectionPoint;
import com.google.common.collect.ImmutableList;

public class PowerSwitchRendering extends SimpleEntityRendering<BSEntity> {
	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		bind.animation(lua.get("power_on_animation"));
	}

	@Override
	public void defineWirePoints(BiConsumer<Integer, WirePoints> consumer, LuaTable lua) {
		FPWireConnectionPoint circuitPoint = new FPWireConnectionPoint(lua.get("circuit_wire_connection_point"));
		FPWireConnectionPoint leftPoint = new FPWireConnectionPoint(lua.get("left_wire_connection_point"));
		FPWireConnectionPoint rightPoint = new FPWireConnectionPoint(lua.get("right_wire_connection_point"));

		consumer.accept(1, WirePoints.fromWireConnectionPoints(ImmutableList.of(circuitPoint), WireColor.RED, true));
		consumer.accept(2, WirePoints.fromWireConnectionPoints(ImmutableList.of(circuitPoint), WireColor.GREEN, true));
		consumer.accept(5, WirePoints.fromWireConnectionPoints(ImmutableList.of(leftPoint), WireColor.COPPER, true));
		consumer.accept(6, WirePoints.fromWireConnectionPoints(ImmutableList.of(rightPoint), WireColor.COPPER, true));
	}
}
