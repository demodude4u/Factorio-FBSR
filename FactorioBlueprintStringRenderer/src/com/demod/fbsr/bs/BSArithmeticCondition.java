package com.demod.fbsr.bs;

import java.util.Optional;
import java.util.OptionalDouble;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;

public class BSArithmeticCondition {
	public final BSSignalID firstSignal;
	public final Optional<BSSignalID> secondSignal;
	public final OptionalDouble secondConstant;
	public final String operation;
	public final BSSignalID outputSignal;
	public final Optional<BSNetworkPorts> firstSignalNetworks;
	public final Optional<BSNetworkPorts> secondSignalNetworks;

	public BSArithmeticCondition(JSONObject json) {
		firstSignal = new BSSignalID(json.getJSONObject("first_signal"));
		secondSignal = BSUtils.opt(json, "second_signal", BSSignalID::new);
		// TODO check if this should be int or double
		secondConstant = BSUtils.optDouble(json, "second_constant");
		operation = json.getString("operation");
		outputSignal = new BSSignalID(json.getJSONObject("output_signal"));
		firstSignalNetworks = BSUtils.opt(json, "first_signal_networks", BSNetworkPorts::new);
		secondSignalNetworks = BSUtils.opt(json, "second_signal_networks", BSNetworkPorts::new);
	}
}
