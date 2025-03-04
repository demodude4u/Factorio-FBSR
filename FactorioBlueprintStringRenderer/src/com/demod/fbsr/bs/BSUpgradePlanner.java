package com.demod.fbsr.bs;

import java.util.List;
import java.util.Optional;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.map.MapVersion;
import com.google.common.collect.ImmutableList;

public class BSUpgradePlanner {
	public final Optional<String> label;
	public final MapVersion version;
	public final Optional<String> description;
	public final Optional<List<BSIcon>> icons;
	public final List<BSUpgradeMapping> mappers;

	public BSUpgradePlanner(JSONObject json) {
		label = BSUtils.optString(json, "label");
		version = new MapVersion(json.getInt("version"));

		JSONObject jsonSettings = json.optJSONObject("settings");
		if (jsonSettings != null) {
			description = BSUtils.optString(jsonSettings, "description");
			icons = BSUtils.optList(jsonSettings, "icons", BSIcon::new);
			mappers = BSUtils.list(jsonSettings, "mappers", BSUpgradeMapping::new);
		} else {
			description = Optional.empty();
			icons = Optional.empty();
			mappers = ImmutableList.of();
		}
	}
}
