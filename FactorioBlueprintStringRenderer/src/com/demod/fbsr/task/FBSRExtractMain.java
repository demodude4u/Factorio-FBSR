package com.demod.fbsr.task;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.Utils;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.factorio.prototype.ItemPrototype;

public class FBSRExtractMain {

	// Change as you like to get what information you need
	public static void main(String[] args) throws Exception {
		DataTable table = FactorioData.getTable();

		ItemPrototype proto = table.getItem("aai-v3-loader").get();
		LuaValue luaIcons = proto.lua().get("icons");
		LuaValue luaIcon = luaIcons.get(2);
		BufferedImage image = FactorioData.getModImage(luaIcon.get("icon").tojstring());
		Color tint = Utils.parseColor(luaIcon.get("tint"));
		BufferedImage imageTinted = Utils.tintImage(image, tint);
		BufferedImage combinationImage = new BufferedImage(image.getWidth() * 3, image.getHeight(),
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = combinationImage.createGraphics();
		g.drawImage(image, 0, 0, null);
		g.drawImage(imageTinted, image.getWidth(), 0, null);
		g.setColor(tint);
		g.fillRect(image.getWidth() * 2, 0, image.getWidth(), image.getHeight());
		g.dispose();
		File folderExport = new File("export-sprites");
		folderExport.mkdirs();
		File fileImage = new File(folderExport, "_debug-tint.png");
		ImageIO.write(combinationImage, "PNG", fileImage);
		Desktop.getDesktop().open(fileImage);

//		String[] entities = { "cube-antimatter-reactor", "cube-annihilation-chamber", "cube-medium-container",
//				"cube-big-container", "cube-fluid-storage-1", "cube-fluid-storage-2", "cube-greenhouse", "cube-crusher",
//				"cube-forbidden-ziggurat", "cube-v4-transport-belt", "cube-deep-core-ultradrill", "cube-roboport",
//				"cube-v4-underground-belt", "cube-v4-splitter", "cube-extremely-long-inserter", "cube-lab",
//				"cube-recovery-bay", "cube-mystery-furnace", "cube-fuel-refinery", "cube-beacon",
//				"cube-stygian-energy-lab", "cube-energy-bulkframe", "cube-electric-mining-drill-mk2",
//				"cube-ultradense-furnace", "cube-particle-phase-aligner", "cube-fluid-burner",
//				"cube-experimental-teleporter", "cube-synthesizer", "cube-fabricator", "cube-quantum-decoder",
//				"cube-cyclotron", "cube-dimension-folding-engine", "aai-v2-loader", "aai-v3-loader", "aai-v4-loader", };
//		for (String entityName : entities) {
//			EntityPrototype proto = table.getEntity(entityName).get();
//			String type = proto.lua().get("type").tojstring();
//			StringBuilder sb = new StringBuilder();
//			for (String part : type.split("-")) {
//				sb.append(part.substring(0, 1).toUpperCase() + part.substring(1));
//			}
//			sb.append("Rendering");
//			System.out.println("\"" + entityName + "\": \"" + sb.toString() + "\",");
//		}

//		System.out.println("Energy Sources with heat type:");
//		table.getEntities().values().stream()
//				.filter(p -> p.lua().get("energy_source").totableObject().get("type").optjstring("").equals("heat"))
//				.forEach(p -> System.out.println("\t" + p.getName() + " (" + p.getType() + ")"));

//		File folder = new File("export-sprites");
//		folder.mkdirs();
//		EntityPrototype prototype = table.getEntity("cargo-bay").get();
//		FPCargoBayConnections protoConnections = new FPCargoBayConnections(
//				prototype.lua().get("graphics_set").get("connections"));
//		for (Field fldVariations : protoConnections.getClass().getFields()) {
//			FPLayeredSpriteVariations protoVariations = (FPLayeredSpriteVariations) fldVariations.get(protoConnections);
//			Field fldLayeredSprites = protoVariations.getClass().getDeclaredField("layeredSprites");
//			fldLayeredSprites.setAccessible(true);
//			List layeredSprites = (List) fldLayeredSprites.get(protoVariations);
//			int varCount = layeredSprites.size();
//			for (int variation = 0; variation < varCount; variation++) {
//				List<SpriteWithLayer> sprites = protoVariations.createSpritesWithLayers(variation);
//				for (SpriteWithLayer swl : sprites) {
//					Sprite sprite = swl.getSprite();
//
//					if (sprite.shadow) {
//						continue;
//					}
//
//					Rectangle source = sprite.source;
//					BufferedImage imageSheet = sprite.image;
//					Rectangle2D.Double bounds = sprite.bounds;
//					BufferedImage image = new BufferedImage(source.width, source.height, imageSheet.getType());
//					Graphics2D g = image.createGraphics();
//					g.drawImage(imageSheet, 0, 0, source.width, source.height, source.x, source.y,
//							source.x + source.width, source.y + source.height, null);
//
//					// Thanks ChatGPT
//					int[] pix = new int[image.getWidth() * image.getHeight()];
//					boolean isEmpty = !new PixelGrabber(image, 0, 0, image.getWidth(), image.getHeight(), pix, 0,
//							image.getWidth()).grabPixels() || Arrays.stream(pix).allMatch(p -> (p >> 24) == 0);
//
//					if (isEmpty) {
//						continue;
//					}
//
//					int originX = (int) ((-bounds.x / bounds.width) * image.getWidth());
//					int originY = (int) ((-bounds.y / bounds.height) * image.getHeight());
//					g.setColor(Color.red);
//					g.drawOval(originX - 5, originY - 5, 10, 10);
//					g.dispose();
//					File file = new File(folder,
//							fldVariations.getName() + "_" + variation + "_" + swl.getLayer().name() + ".png");
//					ImageIO.write(image, "PNG", file);
//					System.out.println(file.getName());
//				}
//			}
//		}
//		Desktop.getDesktop().open(folder);

//		Multimap<String, String> elementPropsByType = ImmutableMultimap.<String, String>builder()//
//				.putAll("activity_bar_style", "bar", "bar_background")//
//				.putAll("empty_widget_style", "graphical_set")//
//				.putAll("frame_style", "graphical_set", "header_background", "background_graphical_set")//
//				.putAll("glow_style", "image_set")//
//				.putAll("image_style", "graphical_set")//
//				.putAll("other_colors", "bar")//
//				.putAll("progress_bar_style", "bar", "bar_background")//
//				.putAll("scroll_bar_style", "background_graphical_set")//
//				.putAll("scroll_pane_style", "graphical_set", "background_graphical_set")//
//				.putAll("slider_style", "full_bar", "full_bar_disabled", "empty_bar", "empty_bar_disabled", "notch")//
//				.putAll("speech_bubble_style", "arrow_graphical_set")//
//				.putAll("style_with_clickable_graphical_set", "default_graphical_set", "hovered_graphical_set",
//						"clicked_graphical_set", "disabled_graphical_set", "selected_graphical_set",
//						"selected_hovered_graphical_set", "game_controller_selected_hovered_graphical_set",
//						"selected_clicked_graphical_set")//
//				.putAll("tab_style", "left_edge_selected_graphical_set", "right_edge_selected_graphical_set",
//						"default_badge_graphical_set", "selected_badge_graphical_set", "hover_badge_graphical_set",
//						"press_badge_graphical_set", "disabled_badge_graphical_set")//
//				.putAll("table_style", "column_graphical_set", "default_row_graphical_set", "even_row_graphical_set",
//						"odd_row_graphical_set", "hovered_graphical_set", "clicked_graphical_set",
//						"selected_graphical_set", "selected_hovered_graphical_set", "selected_clicked_graphical_set",
//						"background_graphical_set")//
//				.putAll("technology_slot_style", "highlighted_graphical_set", "default_background_shadow", "level_band",
//						"hovered_level_band", "level_range_band", "hovered_level_range_band",
//						"default_ingredients_background", "hovered_ingredients_background",
//						"clicked_ingredients_background", "disabled_ingredients_background",
//						"highlighted_ingredients_background", "clicked_overlay", "progress_bar_background",
//						"progress_bar", "progress_bar_shadow")//
//				.putAll("text_box_style", "default_background", "active_background",
//						"game_controller_hovered_background", "disabled_background")//
//				.build();
//		LuaValue luaGUI = table.getRaw("gui-style", "default").get();
//		Utils.forEach(luaGUI, (k, v) -> {
//			if (v.istable()) {
//				LuaValue luaType = v.get("type");
//				if (!luaType.isnil()) {
//					String type = luaType.tojstring();
//					for (String prop : elementPropsByType.get(type)) {
//						LuaValue luaProp = v.get(prop);
//						if (luaProp.isnil()) {
//							continue;
//						}
//						List<LuaValue> layers = new ArrayList<>();
//						layers.add(luaProp.get("base"));
//						layers.add(luaProp.get("shadow"));
//						layers.add(luaProp.get("glow"));
//						layers.removeIf(l -> l.isnil());
//						if (layers.isEmpty()) {
//							layers.add(luaProp);
//						}
//						for (LuaValue luaLayer : layers) {
//							if (luaLayer.get("corner_size").isnil()) {
//								System.out.println(k.tojstring() + "." + prop + " -- (no corner size)");
//							} else {
//								System.out.println(k.tojstring() + "." + prop + " -- "
//										+ new FPVector(luaLayer.get("position")).createPoint() + " "
//										+ luaLayer.get("corner_size"));
//							}
//						}
//					}
//				}
//			}
//		});

//		class Search {
//			private void debugListLua(String prefix, LuaValue value, PrintStream ps, boolean dontPrintUnlessTypeFound) {
//				boolean hasType = !value.get("type").isnil();
//				if (!dontPrintUnlessTypeFound || hasType) {
//					System.out.println(prefix);
//					ps.println(prefix);
//				}
//				if (hasType) {
//					dontPrintUnlessTypeFound = true;
//				}
//				final boolean dputf = dontPrintUnlessTypeFound;
//				Utils.forEachSorted(value, (k, v) -> {
//					if (v.istable()) {
//						debugListLua(prefix + "." + k, v, ps, dputf);
//					}
//				});
//			}
//		}
//		try (PrintStream ps = new PrintStream("raw.txt")) {
//			Search search = new Search();
//			Utils.forEachSorted(table.getRawLua(), (k, v) -> {
//				if (v.istable()) {
//					search.debugListLua(k.tojstring(), v, ps, false);
//				}
//			});
//		}

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
