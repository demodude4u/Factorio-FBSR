package com.demod.fbsr.bs;

import java.util.List;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.google.common.collect.ImmutableList;

public class BSSchedule {
	public final List<Integer> locomotives;
	public final List<BSScheduleRecord> scheduleRecords;

	public BSSchedule(JSONObject json) {
		locomotives = BSUtils.listInt(json, "locomotives");
		if (json.isNull("schedule")) {
			scheduleRecords = ImmutableList.of();
		} else {
			scheduleRecords = BSUtils.list(json.getJSONObject("schedule"), "records", BSScheduleRecord::new);
		}
	}
}
