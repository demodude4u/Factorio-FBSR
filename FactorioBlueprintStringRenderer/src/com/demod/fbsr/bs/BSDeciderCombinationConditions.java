package com.demod.fbsr.bs;

import java.util.List;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;

public class BSDeciderCombinationConditions {
	public final List<BSDeciderCondition> conditions;
	public final List<BSConditionOutput> outputs;

	public BSDeciderCombinationConditions(JSONObject json) {
		conditions = BSUtils.list(json, "conditions", BSDeciderCondition::new);
		outputs = BSUtils.list(json, "outputs", BSConditionOutput::new);
	}
}
