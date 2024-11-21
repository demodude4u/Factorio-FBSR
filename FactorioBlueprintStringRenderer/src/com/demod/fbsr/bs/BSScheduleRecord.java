package com.demod.fbsr.bs;

import java.util.List;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;

public class BSScheduleRecord {
	public final String station;
	public final List<BSScheduleRecordWaitCondition> waitConditions;

	public BSScheduleRecord(JSONObject json) {
		station = json.getString("station");
		waitConditions = BSUtils.list(json, "wait_conditions", BSScheduleRecordWaitCondition::new);
	}
}
