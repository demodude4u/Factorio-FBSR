package com.demod.fbsr.bs;

import java.util.Optional;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;

public class BSParameter {
	public final String type;
	public final Optional<String> name;
	public final int id;
	public final String number;
	public final Optional<String> variable;
	public final Optional<String> formula;
	public final boolean dependent;
	public final boolean notParametrised;
	public final Optional<BSParameterQualityCondition> qualityCondition;
	public final Optional<String> ingredientOf;

	public BSParameter(JSONObject json) {
		type = json.getString("type");
		name = BSUtils.optString(json, "name");
		id = json.optInt("id");
		number = json.optString("number");
		variable = BSUtils.optString(json, "variable");
		formula = BSUtils.optString(json, "formula");
		dependent = json.optBoolean("dependent");
		notParametrised = json.optBoolean("not-parametrised");
		qualityCondition = BSUtils.opt(json, "quality-condition", BSParameterQualityCondition::new);
		ingredientOf = BSUtils.optString(json, "ingredient-of");
	}
}
