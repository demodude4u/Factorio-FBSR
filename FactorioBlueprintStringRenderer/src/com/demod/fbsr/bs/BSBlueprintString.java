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
		blueprintString = blueprintString.trim().replaceAll("\\r|\\n", "");// XXX done twice
		return new BSBlueprintString(decodeRaw(blueprintString), blueprintString);
	}

	public static JSONObject decodeRaw(String blueprintString) throws IOException {
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

		return json;
	}

	public final Optional<BSBlueprint> blueprint;
	public final Optional<BSBlueprintBook> blueprintBook;
	public final Optional<BSUpgradePlanner> upgradePlanner;
	public final Optional<BSDeconstructionPlanner> deconstructionPlanner;

	public final OptionalInt index;

	private final Optional<String> raw;

	public BSBlueprintString(JSONObject json) {
		this(json, Optional.empty());
	}

	private BSBlueprintString(JSONObject json, Optional<String> raw) {
		blueprint = BSUtils.opt(json, "blueprint", BSBlueprint::new);
		blueprintBook = BSUtils.opt(json, "blueprint_book", BSBlueprintBook::new);
		upgradePlanner = BSUtils.opt(json, "upgrade_planner", BSUpgradePlanner::new);
		deconstructionPlanner = BSUtils.opt(json, "deconstruction_planner", BSDeconstructionPlanner::new);
		index = BSUtils.optInt(json, "index");

		this.raw = raw;
	}

	public BSBlueprintString(JSONObject json, String raw) {
		this(json, Optional.of(raw));
	}

	public List<BSBlueprint> findAllBlueprints() {
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

	public Optional<String> findFirstLabel() {
		if (blueprint.isPresent()) {
			BSBlueprint bp = blueprint.get();
			if (bp.label.isPresent()) {
				return bp.label;
			}
		}
		if (blueprintBook.isPresent()) {
			BSBlueprintBook bp = blueprintBook.get();
			if (bp.label.isPresent()) {
				return bp.label;
			}
			return bp.blueprints.stream().flatMap(bs -> bs.findFirstLabel().stream()).findFirst();
		}
		return Optional.empty();
	}

	public Optional<String> getRaw() {
		return raw;
	}
}
