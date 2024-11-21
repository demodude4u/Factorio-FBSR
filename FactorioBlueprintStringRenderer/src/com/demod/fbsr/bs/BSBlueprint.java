package com.demod.fbsr.bs;

import java.util.List;
import java.util.Optional;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.MapVersion;

public class BSBlueprint {
	public final Optional<String> label;
	public final MapVersion version;
	public final Optional<String> description;
	public final List<BSIcon> icons;

	public BSBlueprint(JSONObject json) {
		label = BSUtils.optString(json, "label");
		version = new MapVersion(json.getInt("version"));
		description = BSUtils.optString(json, "description");
		icons = BSUtils.list(json, "icons", BSIcon::new);
		entities = BSUtils.list(json, "entities", BSEntity::new);
		tiles = BSUtils.list(json, "tiles", BSTile::new);
		schedules = BSUtils.list(json, "schedules", BSSchedule::new);
		parameters = BSUtils.list(json, "parameters", BSParameter::new);
		snapToGrid = BSUtils.opt(json, "snap_to_grid", BSPosition::new);
		absoluteSnapping = json.optBoolean("absolute_snapping");
	}
}
