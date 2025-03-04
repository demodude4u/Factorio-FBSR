package com.demod.fbsr.entity;

import java.util.List;
import java.util.function.BiConsumer;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Layer;
import com.demod.fbsr.WirePoints;
import com.demod.fbsr.WirePoints.WireColor;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.entity.BSConstantCombinatorEntity;
import com.demod.fbsr.fp.FPWireConnectionPoint;

public class ConstantCombinatorRendering extends SimpleEntityRendering {

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {

		// this is technically part of EntityWithHealth, but only py has used it here
		Layer integrationPatchLayer = FPUtils.optLayer(lua.get("integration_patch_render_layer"))
				.orElse(Layer.LOWER_OBJECT);
		bind.sprite4Way(lua.get("integration_patch")).layer(integrationPatchLayer);

		bind.sprite4Way(lua.get("sprites"));
	}

	@Override
	public void defineWirePoints(BiConsumer<Integer, WirePoints> consumer, LuaTable lua) {
		List<FPWireConnectionPoint> protoConnectionPoints = FPUtils.list(lua.get("circuit_wire_connection_points"),
				FPWireConnectionPoint::new);

		consumer.accept(1, WirePoints.fromWireConnectionPoints(protoConnectionPoints, WireColor.RED, true));
		consumer.accept(2, WirePoints.fromWireConnectionPoints(protoConnectionPoints, WireColor.GREEN, true));
	}

	// TODO what am I doing with the custom entity?
	@Override
	public Class<? extends BSEntity> getEntityClass() {
		return BSConstantCombinatorEntity.class;
	}
}
