package com.demod.fbsr.task;

import java.util.Comparator;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.FactorioManager;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

public class FBSRScratchPad {

	private static final Logger LOGGER = LoggerFactory.getLogger(FBSRScratchPad.class);

	// Change as you like to get what information you need
	public static void main(String[] args) throws Exception {
//		Choose what you want to load
//		StartAllServices.main(args);
		FactorioManager.initializePrototypes();
//		FactorioManager.initializeFactories();

		Multiset<String> keyBags = HashMultiset.create();
		for (EntityPrototype entity : FactorioManager.getEntities().stream()
				.sorted(Comparator.comparing(e -> e.getName())).collect(Collectors.toList())) {
			LuaValue lua = entity.lua().get("graphics_set").get("working_visualisations");
			if (!lua.isnil() && lua.isobject()) {
				JSONObject json = (JSONObject) lua.getJson();
				keyBags.addAll(json.keySet());
			}
		}
		keyBags.entrySet().stream().sorted(Comparator.comparing(e -> e.getCount())).forEach(e -> {
			LOGGER.debug("{}: {}", e.getElement(), e.getCount());
		});

//		// List out all of the entities and tiles that are placed down by items
//		for (EntityPrototype entity : FactorioManager.getEntities().values().stream()
//				.sorted(Comparator.comparing(e -> e.getName())).collect(Collectors.toList())) {
//			List<ItemToPlace> placeBy = entity.getPlacedBy();
//			if (placeBy.size() > 0) {
//				LOGGER.info(entity.getName() + " PLACED BY "
//						+ placeBy.stream().map(i -> i.getItem() + (i.getCount() > 1 ? ("(" + i.getCount() + ")") : ""))
//								.sorted().collect(Collectors.joining(",", "[", "]")));
//			}
//		}
//		LOGGER.info("");
//		for (TilePrototype tile : FactorioManager.getTiles().values().stream()
//				.sorted(Comparator.comparing(e -> e.getName())).collect(Collectors.toList())) {
//			List<ItemToPlace> placeBy = tile.getPlacedBy();
//			if (placeBy.size() > 0) {
//				LOGGER.info(tile.getName() + " PLACED BY "
//						+ placeBy.stream().map(i -> i.getItem() + (i.getCount() > 1 ? ("(" + i.getCount() + ")") : ""))
//								.sorted().collect(Collectors.joining(",", "[", "]")));
//			}
//		}

////		Generate mod-download.json based on mods folder
//		File folderMods = new File("C:\\Factorio Installs\\Git\\Factorio-BPBot-Mods\\mods-aai");
//		JSONObject json = new JSONObject();
//		Utils.terribleHackToHaveOrderedJSONObject(json);
//		for (File file : Arrays.asList(folderMods.listFiles()).stream().sorted(Comparator.comparing(f -> f.getName()))
//				.collect(Collectors.toList())) {
//			if (!file.getName().endsWith(".zip")) {
//				continue;
//			}
//			String[] split = file.getName().substring(0, file.getName().length() - 4).split("_");
//			json.put(split[0], split[1]);
//		}
//		System.out.println(json.toString(4));

////		Fetch a prototype
//		File fileProto = new File("tempdata/proto.txt");
//		fileProto.getParentFile().mkdirs();
//		try (PrintStream ps = new PrintStream(fileProto)) {
//			Utils.debugPrintLua(FactorioManager.lookupEntityByName("solar-tower-panel0").get().lua().tovalue(), ps);
//		}
//		Desktop.getDesktop().edit(fileProto);

////		 Extract entity types and generate lines for mods-rendering.json
//		String cfgFactorioInstall = "C:\\Factorio Installs\\Factorio 2.0.28";
//		String cfgModsFolder = "C:\\Factorio Installs\\Git\\Factorio-BPBot-Mods\\mods-yuoki";
//		String[] cfgBlueprints = { //
//				"https://gist.github.com/demodude4u/d0f9760757129af782003e3e1fa1b98a", //
//		};
//		File folderData = new File("tempdata");
//		folderData.deleteOnExit();
//		JSONObject config = new JSONObject();
//		config.put("factorio", cfgFactorioInstall);
//		config.put("data", folderData.getAbsolutePath());
//		config.put("mods", cfgModsFolder);
//		FactorioData factorioData = new FactorioData(config);
//		factorioData.initialize();
//		DataTable table = factorioData.getTable();
//		CommandReporting reporting = new CommandReporting(null, null, null);
//		List<BSBlueprintString> bpStrings = BlueprintFinder
//				.search(Arrays.asList(cfgBlueprints).stream().collect(Collectors.joining(" ")), reporting);
//		reporting.getExceptions().forEach(e -> e.printStackTrace());
//		Set<String> entities = new HashSet<>();
//		Set<String> tiles = new HashSet<>();
//		Set<String> missing = new LinkedHashSet<>();
//		for (BSBlueprintString blueprintString : bpStrings) {
//			for (BSBlueprint blueprint : blueprintString.findAllBlueprints()) {
//				for (BSEntity entity : blueprint.entities) {
//					if (entities.add(entity.name)) {
//						LOGGER.info("CHECK ENTITY {}", entity.name);
//						Optional<EntityPrototype> proto = table.getEntity(entity.name);
//						if (proto.isEmpty()) {
//							missing.add(entity.name);
//							entities.remove(entity.name);
//						}
//					}
//				}
//				for (BSTile tile : blueprint.tiles) {
//					if (tiles.add(tile.name)) {
//						LOGGER.info("CHECK TILE {}", tile.name);
//						Optional<TilePrototype> proto = table.getTile(tile.name);
//						if (proto.isEmpty()) {
//							missing.add(tile.name);
//							tiles.remove(tile.name);
//						}
//					}
//				}
//			}
//		}
//		LOGGER.info("");
//		for (String entityName : entities.stream().sorted().collect(Collectors.toList())) {
//			EntityPrototype proto = table.getEntity(entityName).get();
//			String type = proto.lua().get("type").tojstring();
//			StringBuilder sb = new StringBuilder();
//			for (String part : type.split("-")) {
//				sb.append(part.substring(0, 1).toUpperCase() + part.substring(1));
//			}
//			sb.append("Rendering");
//			LOGGER.info("\t\t\"{}\": \"{}\",", entityName, sb.toString());
//		}
//		LOGGER.info("");
//		for (String tileName : tiles.stream().sorted().collect(Collectors.toList())) {
//			LOGGER.info("\t\t\"{}\"", tileName);
//		}
//		LOGGER.info("");
//		missing.forEach(s -> LOGGER.warn("MISSING {}", s));

////		Check tint result applied to an image
//		ItemPrototype proto = table.getItem("aai-v3-loader").get();
//		LuaValue luaIcons = proto.lua().get("icons");
//		LuaValue luaIcon = luaIcons.get(2);
//		BufferedImage image = table.getFactorio().getModImage(luaIcon.get("icon").tojstring());
//		Color tint = Utils.parseColor(luaIcon.get("tint"));
//		BufferedImage imageTinted = Utils.tintImage(image, tint);
//		BufferedImage combinationImage = new BufferedImage(image.getWidth() * 3, image.getHeight(),
//				BufferedImage.TYPE_INT_ARGB);
//		Graphics2D g = combinationImage.createGraphics();
//		g.drawImage(image, 0, 0, null);
//		g.drawImage(imageTinted, image.getWidth(), 0, null);
//		g.setColor(tint);
//		g.fillRect(image.getWidth() * 2, 0, image.getWidth(), image.getHeight());
//		g.dispose();
//		File folderExport = new File("export-sprites");
//		folderExport.mkdirs();
//		File fileImage = new File(folderExport, "_debug-tint.png");
//		ImageIO.write(combinationImage, "PNG", fileImage);
//		Desktop.getDesktop().open(fileImage);

////		Dump sprite images with shift fiducials
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

	}

}
