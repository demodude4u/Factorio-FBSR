package com.demod.fbsr.bs.entity;

import java.util.Optional;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.control.BSTransportBeltControlBehavior;
import com.demod.fbsr.legacy.LegacyBlueprintEntity;

public class BSTransportBeltEntity extends BSEntity {
	public final Optional<BSTransportBeltControlBehavior> controlBehavior;

	public BSTransportBeltEntity(JSONObject json) {
		super(json);

		controlBehavior = BSUtils.opt(json, "control_behavior", BSTransportBeltControlBehavior::new);
	}

	public BSTransportBeltEntity(LegacyBlueprintEntity legacy) {
		super(legacy);

		// TODO need to figure out what is important here
		// constructing with empty json object on purpose
		controlBehavior = BSUtils.opt(legacy.json(), "connections", j -> new BSTransportBeltControlBehavior(new JSONObject()));
	}
}