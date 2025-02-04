package com.demod.fbsr.bs;

import java.util.Optional;
import java.util.OptionalInt;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;

public class BSScheduleRecordWaitCondition {
	public final String compareType;
	public final String type;
	public final Optional<BSCircuitCondition> condition;
	public final OptionalInt ticks;

	public BSScheduleRecordWaitCondition(JSONObject json) {
		compareType = json.getString("compare_type");
		type = json.getString("type");
		condition = BSUtils.opt(json, "condition", BSCircuitCondition::new);
		ticks = BSUtils.optInt(json, "ticks");
	}
}
