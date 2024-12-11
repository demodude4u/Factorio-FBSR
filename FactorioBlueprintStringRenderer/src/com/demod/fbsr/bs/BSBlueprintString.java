package com.demod.fbsr.bs;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.zip.InflaterInputStream;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.google.common.base.Charsets;

//Based on https://github.com/motlin/factorio-blueprint-playground/blob/main/src/parsing/types.ts
// Thanks FactorioBlueprints/motlin!

public class BSBlueprintString {
	public static BSBlueprintString decode(String blueprintString) throws IOException {
		blueprintString = blueprintString.trim().replaceAll("\\r|\\n", "");

		String versionChar = blueprintString.substring(0, 1);
		try {
			if (Integer.parseInt(versionChar) != 0) {
				throw new IllegalArgumentException("Malformed blueprint string!");
			}
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Malformed blueprint string!");
		}

		byte[] decoded = Base64.decodeBase64(blueprintString.substring(1));
		JSONObject json;
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(new InflaterInputStream(new ByteArrayInputStream(decoded)), Charsets.UTF_8))) {
			StringBuilder jsonBuilder = new StringBuilder();
			String line;
			while ((line = br.readLine()) != null) {
				jsonBuilder.append(line);
			}
			json = new JSONObject(jsonBuilder.toString());
		}

		return new BSBlueprintString(json);
	}

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

	public List<BSBlueprint> getAllBlueprints() {
		List<BSBlueprint> ret = new ArrayList<>();

		ArrayDeque<BSBlueprintString> work = new ArrayDeque<>();
		work.add(this);
		while (!work.isEmpty()) {
			BSBlueprintString bs = work.poll();
			if (bs.blueprint.isPresent()) {
				ret.add(bs.blueprint.get());
			}
			if (bs.blueprintBook.isPresent()) {
				bs.blueprintBook.get().blueprints.stream().forEach(work::add);
			}
		}

		return ret;
	}
}
