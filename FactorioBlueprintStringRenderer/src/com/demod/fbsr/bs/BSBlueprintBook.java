package com.demod.fbsr.bs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;

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

	public List<BSBlueprint> getAllBlueprints() {
		List<BSBlueprint> ret = new ArrayList<>();
		getAllBlueprints(ret::add);
		return ret;
	}

	public void getAllBlueprints(Consumer<BSBlueprint> consumer) {
		for (BSBlueprintString blueprintString : blueprints) {
			blueprintString.blueprint.ifPresent(consumer);
			blueprintString.blueprintBook.stream()
					.sorted(Comparator.comparing(b -> b.activeIndex.orElse(Integer.MAX_VALUE)))
					.forEach(b -> b.getAllBlueprints(consumer));
		}
	}
}
