package com.demod.fbsr.entity;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.json.JSONObject;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.BSUtils;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Layer;
import com.demod.fbsr.WirePoints;
import com.demod.fbsr.WirePoints.WireColor;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.entity.ConstantCombinatorRendering.BSConstantCombinatorEntity;
import com.demod.fbsr.fp.FPWireConnectionPoint;
import com.demod.fbsr.legacy.LegacyBlueprintEntity;

public class ConstantCombinatorRendering extends SimpleEntityRendering<BSConstantCombinatorEntity> {

	public static class BSConstantCombinatorEntity extends BSEntity {
		public final Optional<String> playerDescription;

		public BSConstantCombinatorEntity(JSONObject json) {
			super(json);

			playerDescription = BSUtils.optString(json, "player_description");

			// TODO sections in control_behavior
//			if (json.has("control_behavior")) {
//				JSONObject jsonControlBehavior = json.getJSONObject("control_behavior");

//			} else {

//			}
		}

		public BSConstantCombinatorEntity(LegacyBlueprintEntity legacy) {
			super(legacy);

			playerDescription = Optional.empty();
		}
	}

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
}
