package com.demod.fbsr.bs.control;

import java.util.Optional;
import java.util.OptionalInt;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.bs.BSFilter;
import com.demod.fbsr.bs.BSSignalID;

public class BSSelectorCombinatorControlBehavior {

	public final Optional<String> operation;

	public final Optional<BSSignalID> countSignal;
	public final Optional<Boolean> selectMax;
	public final Optional<BSSignalID> indexSignal;
	public final OptionalInt randomUpdateInterval;
	public final Optional<BSFilter> qualityFilter;
	public final Optional<BSSignalID> qualitySourceSignal;
	public final Optional<BSSignalID> qualityDestinationSignal;

	public BSSelectorCombinatorControlBehavior(JSONObject json) {
		operation = BSUtils.optString(json, "operation");

		// Operation: count
		countSignal = BSUtils.opt(json, "count_signal", BSSignalID::new);

		// Operation: select
		selectMax = BSUtils.optBool(json, "select_max");
		indexSignal = BSUtils.opt(json, "index_signal", BSSignalID::new);

		// Operation: random
		randomUpdateInterval = BSUtils.optInt(json, "random_update_interval");

		// Operation: quality-filter
		qualityFilter = BSUtils.opt(json, "quality_filter", BSFilter::new);

		// Operation: quality-transfer
		qualitySourceSignal = BSUtils.opt(json, "quality_source_signal", BSSignalID::new);
		qualityDestinationSignal = BSUtils.opt(json, "quality_destination_signal", BSSignalID::new);
	}
}
