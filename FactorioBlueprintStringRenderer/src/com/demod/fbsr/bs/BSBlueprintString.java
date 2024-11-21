package com.demod.fbsr.bs;

import java.util.Optional;
import java.util.OptionalInt;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;

//Based on https://github.com/motlin/factorio-blueprint-playground/blob/main/src/parsing/types.ts
// Thanks FactorioBlueprints/motlin!

public class BSBlueprintString {
	public final Optional<BSBlueprint> blueprint;
	public final Optional<BSBlueprintBook> blueprintBook;
	public final Optional<BSUpgradePlanner> upgradePlanner;
	public final Optional<BSDeconstructionPlanner> deconstructionPlanner;
	public final OptionalInt index;

	public BSBlueprintString(JSONObject json) {
		blueprint = BSUtils.opt(json, "blueprint", BSBlueprint::new);
		blueprintBook = BSUtils.opt(json, "blueprint_book", BSBlueprintBook::new);
		upgradePlanner = BSUtils.opt(json, "upgrade_planner", BSUpgradePlanner::new);
		deconstructionPlanner = BSUtils.opt(json, "deconstruction_planner", BSDeconstructionPlanner::new);
		index = BSUtils.optInt(json, "index");
	}
}
