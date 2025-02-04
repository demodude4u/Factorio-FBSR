package com.demod.fbsr.bs;

import java.util.Optional;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;

public class BSUpgradeMapping {
	public final Optional<BSSignalID> from;
	public final Optional<BSSignalID> to;
	public final int index;

	public BSUpgradeMapping(JSONObject json) {
		from = BSUtils.opt(json, "from", BSSignalID::new);
		to = BSUtils.opt(json, "to", BSSignalID::new);
		index = json.getInt("index");
	}
}
