package com.demod.fbsr.bs.entity;

import java.util.Optional;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.BSSignalID;
import com.demod.fbsr.legacy.LegacyBlueprintEntity;

public class BSDisplayPanelEntity extends BSEntity {

	public final Optional<BSSignalID> icon;

	public BSDisplayPanelEntity(JSONObject json) {
		super(json);

		icon = BSUtils.opt(json, "icon", BSSignalID::new);
	}

	public BSDisplayPanelEntity(LegacyBlueprintEntity legacy) {
		super(legacy);

		icon = Optional.empty();
	}
}
