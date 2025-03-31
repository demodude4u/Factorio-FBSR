package com.demod.fbsr.bs;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.TagManager;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.map.MapIcon;
import com.demod.fbsr.map.MapPosition;

public class BSFilter {
	public final int index;
	public final Optional<String> name;
	public final Optional<String> type;
	public final Optional<String> quality;
	public final Optional<String> comparator;
	public final OptionalInt count;
	public final OptionalInt maxCount;

	public BSFilter(JSONObject json) {
		index = json.optInt("index", 1);
		name = BSUtils.optString(json, "name");// XXX could be handled better
		type = BSUtils.optString(json, "type");// TODO default value item? enum?
		quality = BSUtils.optString(json, "quality");
		comparator = BSUtils.optString(json, "comparator");
		count = BSUtils.optInt(json, "count");
		maxCount = BSUtils.optInt(json, "max_count");
	}

	public BSFilter(String legacyName) {
		index = 1;
		name = Optional.of(legacyName);
		type = Optional.empty();
		quality = Optional.empty();
		comparator = Optional.empty();
		count = OptionalInt.empty();
		maxCount = OptionalInt.empty();
	}

	public Optional<MapIcon> createMapIcon(MapPosition position, double size, OptionalDouble border, boolean above) {
		Optional<ImageDef> icon;
		Optional<String> iconQuality;
		if (name.isPresent()) {
			icon = TagManager.lookup("item", name.get());
			iconQuality = quality;
		} else if (quality.isPresent()) {
			icon = TagManager.lookup("quality", quality.get());
			iconQuality = Optional.empty();
		} else {
			icon = Optional.empty();
			iconQuality = Optional.empty();
		}
		if (icon.isPresent()) {
			return Optional.of(new MapIcon(position, icon.get(), size, border, above, iconQuality));
		} else {
			return Optional.empty();
		}
	}
}
