package com.demod.fbsr.bs;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONObject;

import com.demod.factorio.Utils;
import com.demod.fbsr.BSUtils;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.ModdingResolver;
import com.demod.fbsr.Profile;
import com.demod.fbsr.Profile.ManifestModInfo;
import com.demod.fbsr.TileRendererFactory;
import com.demod.fbsr.legacy.LegacyBlueprint;
import com.demod.fbsr.map.MapVersion;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

public class BSBlueprint {

	public final Optional<String> label;
	public final MapVersion version;
	public final Optional<String> description;
	public final List<BSIcon> icons;
	public final List<BSMetaEntity> entities;
	public final List<BSTile> tiles;
	public final List<BSSchedule> schedules;
	public final List<BSParameter> parameters;
	public final Optional<BSPosition> snapToGrid;
	public final Optional<BSPosition> positionRelativeToGrid;
	public final boolean absoluteSnapping;
	public final List<BSWire> wires;

	public BSBlueprint(JSONObject json) {
		version = new MapVersion(json.getLong("version"));

		if (version.compareTo(new MapVersion(2, 0, 0, 0)) < 0) {

			LegacyBlueprint legacyBlueprint = new LegacyBlueprint(json);

			// TODO look at older blueprints and see if I can extract more info
			label = legacyBlueprint.label;
			description = Optional.empty();
			icons = ImmutableList.of();
			entities = legacyBlueprint.entities.stream().map(e -> new BSMetaEntity(e)).collect(Collectors.toList());
			tiles = legacyBlueprint.tiles.stream().map(e -> new BSTile(e)).collect(Collectors.toList());
			schedules = ImmutableList.of();
			parameters = ImmutableList.of();
			snapToGrid = Optional.empty();
			positionRelativeToGrid = Optional.empty();
			absoluteSnapping = false;
			wires = ImmutableList.of();// TODO

		} else {
			label = BSUtils.optString(json, "label");
			description = BSUtils.optString(json, "description");
			icons = BSUtils.list(json, "icons", BSIcon::new);
			entities = BSUtils.list(json, "entities", BSMetaEntity::new);
			tiles = BSUtils.list(json, "tiles", BSTile::new);
			schedules = BSUtils.list(json, "schedules", BSSchedule::new);
			parameters = BSUtils.list(json, "parameters", BSParameter::new);
			snapToGrid = BSUtils.optPosition(json, "snap-to-grid");
			positionRelativeToGrid = BSUtils.optPosition(json, "position-relative-to-grid");
			absoluteSnapping = json.optBoolean("absolute-snapping");

			if (json.has("wires")) {
				Builder<BSWire> wires = ImmutableList.builder();
				Utils.forEach(json.getJSONArray("wires"), (JSONArray j) -> {
					wires.add(new BSWire(j));
				});
				this.wires = wires.build();
			} else {
				this.wires = ImmutableList.of();
			}
		}
	}

	public static class BlueprintModInfo {
		public final List<String> spaceAgeMods;
		public final List<String> mods;
		
		public BlueprintModInfo(List<String> spaceAgeMods, List<String> mods) {
			this.spaceAgeMods = ImmutableList.copyOf(spaceAgeMods);
			this.mods = ImmutableList.copyOf(mods);
		}
	}
	public static final List<String> SPACE_AGE_MODS = ImmutableList.of("space-age", "elevated-rails", "quality");
    public BlueprintModInfo loadModInfo(ModdingResolver resolver) {
        Set<String> spaceAgeMods = new LinkedHashSet<>();
        Set<String> mods = new LinkedHashSet<>();
		Consumer<ManifestModInfo> addMod = mod -> {
			if (SPACE_AGE_MODS.contains(mod.name)) {
				spaceAgeMods.add(mod.title);
			} else {
				mods.add(mod.title);
			}
		};
        entities.stream().map(e -> e.name).distinct().forEach(name -> {
			EntityRendererFactory factory = resolver.resolveFactoryEntityName(name);
			if (factory.isUnknown()) {
				mods.add("Modded");
			} else {
				factory.getMods().forEach(addMod);
			}
		});
		tiles.stream().map(t -> t.name).distinct().forEach(name -> {
			TileRendererFactory factory = resolver.resolveFactoryTileName(name);
			if (factory.isUnknown()) {
				mods.add("Modded");
			} else {
				factory.getMods().forEach(addMod);
			}
		});
		return new BlueprintModInfo(
			spaceAgeMods.stream().sorted().collect(Collectors.toList()),
			mods.stream().sorted().collect(Collectors.toList())
		);
	}
}
