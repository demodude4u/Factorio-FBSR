package com.demod.fbsr.bs;

import java.util.List;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.google.common.collect.ImmutableList;

public class BSDeciderConditions {
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