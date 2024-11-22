package com.demod.fbsr.bs;

import java.util.List;
import java.util.Optional;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;

public class BSConditionOutput {
	public final BSSignalID signal;
	public final boolean copyCountFromInput;
	public final Optional<List<BSNetworkPorts>> networks;

	public BSConditionOutput(JSONObject json) {
		signal = new BSSignalID(json.getJSONObject("signal"));
		copyCountFromInput = json.optBoolean("copy_count_from_input");
		networks = BSUtils.optList(json, "networks", BSNetworkPorts::new);
	}
}
