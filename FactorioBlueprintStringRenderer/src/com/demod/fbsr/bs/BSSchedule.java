package com.demod.fbsr.bs;

import java.util.List;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;

public class BSSchedule {
	public final List<Integer> locomotives;
	public final List<BSScheduleRecord> scheduleRecords;

	public BSSchedule(JSONObject json) {
		locomotives = BSUtils.list(json, "locomotives", JSONObject::getInt);
		scheduleRecords = BSUtils.list(json.getJSONObject("schedule"), "records", BSScheduleRecord::new);
	}
}
