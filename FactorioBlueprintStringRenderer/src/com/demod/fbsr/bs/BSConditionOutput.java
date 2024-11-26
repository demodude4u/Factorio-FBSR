package com.demod.fbsr.bs;

import java.util.Optional;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;

public class BSConditionOutput {
	public final Optional<BSSignalID> signal;
	public final boolean copyCountFromInput;
	public final Optional<BSNetworkPorts> networks;

	public BSConditionOutput(JSONObject json) {
		signal = BSUtils.opt(json, "signal", BSSignalID::new);
		copyCountFromInput = json.optBoolean("copy_count_from_input");
		networks = BSUtils.opt(json, "networks", BSNetworkPorts::new);
	}
}
