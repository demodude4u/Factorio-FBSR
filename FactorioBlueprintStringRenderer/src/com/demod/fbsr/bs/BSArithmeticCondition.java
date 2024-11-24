package com.demod.fbsr.bs;

import java.util.Optional;
import java.util.OptionalDouble;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;

public class BSArithmeticCondition {
	public final Optional<BSSignalID> firstSignal;
	public final Optional<BSSignalID> secondSignal;
	public final OptionalDouble firstConstant;
	public final OptionalDouble secondConstant;
	public final String operation;
	public final Optional<BSSignalID> outputSignal;
	public final Optional<BSNetworkPorts> firstSignalNetworks;
	public final Optional<BSNetworkPorts> secondSignalNetworks;

	public BSArithmeticCondition(JSONObject json) {
		firstSignal = BSUtils.opt(json, "first_signal", BSSignalID::new);
		secondSignal = BSUtils.opt(json, "second_signal", BSSignalID::new);
		// TODO check if this should be int or double
		firstConstant = BSUtils.optDouble(json, "first_constant");
		secondConstant = BSUtils.optDouble(json, "second_constant");
		operation = json.getString("operation");
		outputSignal = BSUtils.opt(json, "output_signal", BSSignalID::new);
		firstSignalNetworks = BSUtils.opt(json, "first_signal_networks", BSNetworkPorts::new);
		secondSignalNetworks = BSUtils.opt(json, "second_signal_networks", BSNetworkPorts::new);
	}
}
