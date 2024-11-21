package com.demod.fbsr.bs;

import java.util.List;
import java.util.Optional;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.MapVersion;

public class BSDeconstructionPlanner {
	public final Optional<String> label;
	public final MapVersion version;
	public final Optional<String> description;
	public final List<BSIcon> icons;
	public final List<BSFilter> entityFilters;
	public final int entityFilterMode;// 0 allow, 1 deny
	public final List<BSFilter> tileFilters;
	public final int tileSelectionMode;// 1 default, 2 never, 3 always, ??? only TODO
	public final boolean treesAndRocksOnly;

	// TODO recognize special entities/tiles
	// - Entity ghost
	// - Item on ground
	// - Item request slot
	// - Tile ghost

	public BSDeconstructionPlanner(JSONObject json) {
		label = BSUtils.optString(json, "label");
		version = new MapVersion(json.getInt("version"));

		// TODO what are the defaults?
		JSONObject jsonSettings = json.getJSONObject("settings");
		description = BSUtils.optString(jsonSettings, "description");
		icons = BSUtils.list(jsonSettings, "icons", BSIcon::new);
		entityFilters = BSUtils.list(jsonSettings, "entity_filters", BSFilter::new);
		entityFilterMode = jsonSettings.optInt("entity_filter_mode");
		tileFilters = BSUtils.list(jsonSettings, "tile_filters", BSFilter::new);
		tileSelectionMode = jsonSettings.optInt("tile_selection_mode");
		treesAndRocksOnly = jsonSettings.optBoolean("trees_and_rocks_only");
	}
}
