package com.demod.fbsr.entity;

import java.util.Optional;

import org.json.JSONObject;
import org.luaj.vm2.LuaValue;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.entity.ConstantCombinatorRendering.BSConstantCombinatorEntity;

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
	}

	@Override
	public void defineEntity(Bindings bind, LuaValue lua) {
		bind.sprite4Way(lua.get("sprites"));
	}
}
