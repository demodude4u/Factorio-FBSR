package com.demod.fbsr.bs.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.json.JSONObject;

import com.demod.factorio.Utils;
import com.demod.fbsr.BSUtils;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.BSInfinitySettings;
import com.demod.fbsr.legacy.LegacyBlueprintEntity;

public class BSInfinityContainerEntity extends BSEntity {
	public final Optional<BSInfinitySettings> infinitySettings;

	public BSInfinityContainerEntity(JSONObject json) {
		super(json);

		infinitySettings = BSUtils.opt(json, "infinity_settings", BSInfinitySettings::new);
	}

	public BSInfinityContainerEntity(LegacyBlueprintEntity legacy) {
		super(legacy);

		if (legacy.json().has("infinity_settings")
				&& legacy.json().getJSONObject("infinity_settings").has("filters")) {
			List<String> items = new ArrayList<>();
			Utils.<JSONObject>forEach(legacy.json().getJSONObject("infinity_settings").getJSONArray("filters"),
					j -> {
						if (j.getInt("count") > 0)
							items.add(j.getString("name"));
					});

			infinitySettings = Optional.of(new BSInfinitySettings(items));
		} else {
			infinitySettings = Optional.empty();
		}
	}
}