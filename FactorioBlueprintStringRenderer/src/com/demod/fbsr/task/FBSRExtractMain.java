package com.demod.fbsr.task;

import java.io.PrintStream;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.Utils;

public class FBSRExtractMain {

	// Change as you like to get what information you need
	public static void main(String[] args) throws Exception {
		DataTable table = FactorioData.getTable();

		class Search {
			private void debugListLua(String prefix, LuaValue value, PrintStream ps, boolean dontPrintUnlessTypeFound) {
				boolean hasType = !value.get("type").isnil();
				if (!dontPrintUnlessTypeFound || hasType) {
					System.out.println(prefix);
					ps.println(prefix);
				}
				if (hasType) {
					dontPrintUnlessTypeFound = true;
				}
				final boolean dputf = dontPrintUnlessTypeFound;
				Utils.forEachSorted(value, (k, v) -> {
					if (v.istable()) {
						debugListLua(prefix + "." + k, v, ps, dputf);
					}
				});
			}
		}
		try (PrintStream ps = new PrintStream("raw.txt")) {
			Search search = new Search();
			Utils.forEachSorted(table.getRawLua(), (k, v) -> {
				if (v.istable()) {
					search.debugListLua(k.tojstring(), v, ps, false);
				}
			});
		}

//		String[] checkNames = { "accumulator", "discharge-defense-equipment", "personal-laser-defense-equipment",
//				"agricultural-tower", "gun-turret", "railgun-turret", "rocket-turret", "arithmetic-combinator",
//				"artillery-turret", "artillery-wagon", "chemical-plant", "captive-biter-spawner",
//				"electromagnetic-plant", "foundry", "oil-refinery", "crusher", "centrifuge", "biochamber",
//				"assembling-machine-2", "assembling-machine-1", "assembling-machine-3", "cryogenic-plant",
//				"asteroid-collector", "battery-mk3-equipment", "battery-equipment", "battery-mk2-equipment", "beacon",
//				"belt-immunity-equipment", "blueprint", "blueprint-book", "heat-exchanger", "boiler", "tank", "car",
//				"cargo-bay", "cargo-landing-pad", "cargo-wagon", "constant-combinator", "construction-robot",
//				"steel-chest", "iron-chest", "wooden-chest", "decider-combinator", "deconstruction-planner",
//				"display-panel", "big-electric-pole", "small-electric-pole", "medium-electric-pole", "substation",
//				"tesla-turret", "laser-turret", "energy-shield-equipment", "energy-shield-mk2-equipment",
//				"flamethrower-turret", "fluid-wagon", "recycler", "steel-furnace", "electric-furnace", "stone-furnace",
//				"fusion-generator", "fusion-reactor", "gate", "steam-engine", "steam-turbine",
//				"fission-reactor-equipment", "fusion-reactor-equipment", "heat-pipe", "burner-inserter",
//				"fast-inserter", "inserter", "stack-inserter", "bulk-inserter", "long-handed-inserter",
//				"toolbelt-equipment", "lab", "biolab", "small-lamp", "land-mine", "lightning-collector",
//				"lightning-rod", "locomotive", "passive-provider-chest", "requester-chest", "buffer-chest",
//				"active-provider-chest", "storage-chest", "logistic-robot", "burner-mining-drill", "pumpjack",
//				"big-mining-drill", "electric-mining-drill", "exoskeleton-equipment", "night-vision-equipment",
//				"offshore-pump", "pipe", "pipe-to-ground", "power-switch", "programmable-speaker", "pump", "radar",
//				"rail-chain-signal", "rail", "rail-ramp", "rail-signal", "rail-support", "nuclear-reactor",
//				"heating-tower", "repair-pack", "roboport", "personal-roboport-mk2-equipment",
//				"personal-roboport-equipment", "rocket-silo", "selector-combinator", "solar-panel",
//				"solar-panel-equipment", "space-platform-hub", "space-platform-starter-pack", "spidertron",
//				"spidertron-remote", "turbo-splitter", "express-splitter", "fast-splitter", "splitter", "storage-tank",
//				"thruster", "landfill", "foundation", "artificial-yumako-soil", "overgrowth-yumako-soil",
//				"space-platform-foundation", "overgrowth-jellynut-soil", "artificial-jellynut-soil", "refined-concrete",
//				"concrete", "ice-platform", "train-stop", "express-transport-belt", "transport-belt",
//				"turbo-transport-belt", "fast-transport-belt", "express-underground-belt", "fast-underground-belt",
//				"turbo-underground-belt", "underground-belt", "upgrade-planner", "stone-wall" };
//
//		for (String name : checkNames) {
//			Optional<EntityPrototype> entity = table.getEntity(name);
//			if (entity.isPresent() && EntityRendererFactory.forName(name) == EntityRendererFactory.UNKNOWN) {
//				System.out.println("MISSING ENTITY " + name + " (" + entity.get().getType() + ")");
//			}
//			if (table.getTile(name).isPresent() && TileRendererFactory.forName(name) == TileRendererFactory.UNKNOWN) {
//				System.out.println("MISSING TILE " + name);
//			}
//		}

//		LuaValue luaVirtualSignal = table.getRaw("virtual-signal").get();
//		Utils.forEach(luaVirtualSignal, (k, v) -> {
//			System.out.println(k.tojstring() + " -- " + v.get("icon").tojstring());
//		});

//		try (PrintStream ps = new PrintStream("proto.txt")) {
//			Utils.debugPrintLua(table.getTile("stone-path").get().lua(), ps);
//		}
//		Desktop.getDesktop().edit(new File("proto.txt"));

//		table.getTiles().entrySet().stream()
//				.collect(Collectors.groupingBy(e -> e.getValue().lua().get("layer").checkint())).entrySet().stream()
//				.sorted(Comparator.comparing(e -> e.getKey())).forEach(e -> System.out.println(e.getKey() + ": "
//						+ e.getValue().stream().map(e2 -> e2.getKey()).collect(Collectors.joining(", ", "[", "]"))));
	}

}
