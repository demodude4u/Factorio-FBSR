package com.demod.fbsr.entity;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.entity.SelectorCombinatorRendering.BSSelectorCombinatorEntity;

public class SelectorCombinatorRendering extends CombinatorRendering<BSSelectorCombinatorEntity> {

	public static class BSSelectorCombinatorEntity extends BSEntity {
		public final Optional<String> playerDescription;
		public final Optional<String> operation;
		public final Optional<Boolean> selectMax;
		public final OptionalInt indexConstant;

		public BSSelectorCombinatorEntity(JSONObject json) {
			super(json);

			playerDescription = BSUtils.optString(json, "player_description");

			if (json.has("control_behavior")) {
				JSONObject jsonControlBehavior = json.getJSONObject("control_behavior");
				operation = BSUtils.optString(jsonControlBehavior, "operation");
				selectMax = BSUtils.optBool(jsonControlBehavior, "select_max");
				indexConstant = BSUtils.optInt(jsonControlBehavior, "index_constant");
			} else {
				operation = Optional.empty();
				selectMax = Optional.empty();
				indexConstant = OptionalInt.empty();
			}
		}
	}

	@Override
	public void defineOperations(Map<String, String> operations) {
		operations.put("select", "min_symbol_sprites");// TODO change to max_symbol_sprites if selectMax true
		operations.put("count", "count_symbol_sprites");
		operations.put("random", "random_symbol_sprites");
		operations.put("stack-size", "stack_size_sprites");
		operations.put("rocket-capacity", "rocket_capacity_sprites");
		operations.put("quality-filter", "quality_symbol_sprites");
		operations.put("quality-transfer", "quality_symbol_sprites");
	}

	@Override
	public Optional<String> getOperation(BSSelectorCombinatorEntity entity) {
		return entity.operation;
	}
}
