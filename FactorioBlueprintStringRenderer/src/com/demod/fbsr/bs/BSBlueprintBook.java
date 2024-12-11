package com.demod.fbsr.bs;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.MapVersion;

public class BSBlueprintBook {
	public final Optional<String> label;
	public final MapVersion version;
	public final Optional<String> description;
	public final Optional<List<BSIcon>> icons;
	public final List<BSBlueprintString> blueprints;
	public final OptionalInt activeIndex;

	public BSBlueprintBook(JSONObject json) {
		label = BSUtils.optString(json, "label");
		version = new MapVersion(json.getInt("version"));
		description = BSUtils.optString(json, "description");
		icons = BSUtils.optList(json, "icons", BSIcon::new);
		blueprints = BSUtils.list(json, "blueprints", j -> new BSBlueprintString(j));
		activeIndex = BSUtils.optInt(json, "active_index");
	}
}
