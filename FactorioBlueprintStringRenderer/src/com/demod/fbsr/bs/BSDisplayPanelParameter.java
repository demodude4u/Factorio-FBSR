package com.demod.fbsr.bs;

import java.util.Optional;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;

public class BSDisplayPanelParameter {
	public final BSSignalID firstSignal;
	public final double constant;
	public final String comparator;
	public final Optional<BSSignalID> icon;

	public BSDisplayPanelParameter(JSONObject json) {
		// XXX suspicious json
		JSONObject jsonCondition = json.getJSONObject("condition");
		firstSignal = new BSSignalID(jsonCondition.getJSONObject("first_signal"));
		// TODO check if this should be int or double
		constant = jsonCondition.getDouble("constant");
		comparator = jsonCondition.getString("comparator");
		icon = BSUtils.opt(json, "icon", BSSignalID::new);
	}
}
