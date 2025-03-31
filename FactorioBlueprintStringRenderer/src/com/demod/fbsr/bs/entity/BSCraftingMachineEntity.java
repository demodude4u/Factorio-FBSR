package com.demod.fbsr.bs.entity;

import java.util.Optional;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.legacy.LegacyBlueprintEntity;

public class BSCraftingMachineEntity extends BSEntity {
	public final Optional<String> recipe;
	public final Optional<String> recipeQuality;
	public final boolean mirror;

	public BSCraftingMachineEntity(JSONObject json) {
		super(json);

		recipe = BSUtils.optString(json, "recipe");
		recipeQuality = BSUtils.optString(json, "recipe_quality");
		mirror = json.optBoolean("mirror");
	}

	public BSCraftingMachineEntity(LegacyBlueprintEntity legacy) {
		super(legacy);

		recipe = BSUtils.optString(legacy.json(), "recipe");
		recipeQuality = Optional.empty();
		mirror = false;
	}

}