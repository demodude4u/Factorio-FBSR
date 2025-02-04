package com.demod.fbsr.entity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.bs.BSConditionOutput;
import com.demod.fbsr.bs.BSDeciderCondition;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.entity.DeciderCombinatorRendering.BSDeciderCombinatorEntity;
import com.demod.fbsr.legacy.LegacyBlueprintEntity;
import com.google.common.collect.ImmutableList;

public class DeciderCombinatorRendering extends CombinatorRendering<BSDeciderCombinatorEntity> {

	public static class BSDeciderCombinatorEntity extends BSEntity {
		public final Optional<String> playerDescription;
		public final Optional<BSDeciderConditions> deciderConditions;

		public BSDeciderCombinatorEntity(JSONObject json) {
			super(json);

			playerDescription = BSUtils.optString(json, "player_description");

			if (json.has("control_behavior")) {
				JSONObject jsonControlBehavior = json.getJSONObject("control_behavior");
				deciderConditions = BSUtils.opt(jsonControlBehavior, "decider_conditions", BSDeciderConditions::new);
			} else {
				deciderConditions = Optional.empty();
			}
		}

		public BSDeciderCombinatorEntity(LegacyBlueprintEntity legacy) {
			super(legacy);

			playerDescription = Optional.empty();

			String comparatorString = legacy.json().getJSONObject("control_behavior")
					.getJSONObject("decider_conditions").getString("comparator");
			deciderConditions = Optional.of(new BSDeciderConditions(comparatorString));
		}
	}

	public static class BSDeciderConditions {
		public final List<BSDeciderCondition> conditions;
		public final List<BSConditionOutput> outputs;

		public BSDeciderConditions(JSONObject json) {
			conditions = BSUtils.list(json, "conditions", BSDeciderCondition::new);
			outputs = BSUtils.list(json, "outputs", BSConditionOutput::new);
		}

		public BSDeciderConditions(String legacyComparatorString) {
			conditions = ImmutableList.of(new BSDeciderCondition(legacyComparatorString));
			outputs = ImmutableList.of();
		}
	}

	@Override
	public void defineOperations(Map<String, String> operations) {
		operations.put("=", "equal_symbol_sprites");
		operations.put(">", "greater_symbol_sprites");
		operations.put("<", "less_symbol_sprites");
		operations.put("\u2260", "not_equal_symbol_sprites");
		operations.put("\u2264", "less_or_equal_symbol_sprites");
		operations.put("\u2265", "greater_or_equal_symbol_sprites");

	}

	@Override
	public Optional<String> getOperation(BSDeciderCombinatorEntity entity) {
		return entity.deciderConditions.flatMap(bs -> bs.conditions.stream().findFirst()).map(bs -> bs.comparator);
	}
}
