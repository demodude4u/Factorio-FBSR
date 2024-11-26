package com.demod.fbsr.entity;

import java.util.List;
import java.util.function.BiConsumer;

import org.luaj.vm2.LuaValue;

import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Layer;
import com.demod.fbsr.WirePoints;
import com.demod.fbsr.WirePoints.WireColor;
import com.demod.fbsr.fp.FPWireConnectionPoint;

public class ElectricPoleRendering extends SimpleEntityRendering {

	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.rotatedSprite(lua.get("pictures")).layer(Layer.ENTITY3);
	}

	public void defineWirePoints(BiConsumer<Integer, WirePoints> consumer, LuaValue lua) {
		List<FPWireConnectionPoint> protoConnectionPoints = FPUtils.list(lua.get("connection_points"),
				FPWireConnectionPoint::new);

		consumer.accept(1, WirePoints.fromWireConnectionPoints(protoConnectionPoints, WireColor.RED));
		consumer.accept(2, WirePoints.fromWireConnectionPoints(protoConnectionPoints, WireColor.GREEN));
		consumer.accept(5, WirePoints.fromWireConnectionPoints(protoConnectionPoints, WireColor.COPPER));
	}
}
