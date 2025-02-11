package com.demod.fbsr.task;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONObject;

import com.demod.dcba.CommandReporting;
import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintFinder;
import com.demod.fbsr.bs.BSBlueprint;
import com.demod.fbsr.bs.BSBlueprintString;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.BSTile;

public class FBSRScratchPad {

	// Change as you like to get what information you need
	public static void main(String[] args) throws Exception {
//		StartAllServices.main(args);
//		FactorioManager.initializePrototypes();
//		FactorioManager.initializeFactories();

//		File fileProto = new File("tempdata/proto.txt");
//		fileProto.getParentFile().mkdirs();
//		try (PrintStream ps = new PrintStream(fileProto)) {
//			Utils.debugPrintLua(FactorioManager.lookupEntityByName("creative-mod_super-roboport").get().lua().tovalue(),
//					ps);
//		}
//		Desktop.getDesktop().edit(fileProto);

//		 Extract entity types and generate lines for mods-rendering.json
		String cfgFactorioInstall = "C:\\Factorio Installs\\Factorio 2.0.28";
		String cfgModsFolder = "C:\\Factorio Installs\\Git\\Factorio-BPBot-Mods\\mods-utilities";
		String cfgBlueprint = "https://gist.github.com/demodude4u/197d44056e2d785efec4a39883d858e3";
		File folderData = new File("tempdata");
		folderData.deleteOnExit();
		JSONObject config = new JSONObject();
		config.put("factorio", cfgFactorioInstall);
		config.put("data", folderData.getAbsolutePath());
		config.put("mods", cfgModsFolder);
		FactorioData factorioData = new FactorioData(config);
		factorioData.initialize();
		DataTable table = factorioData.getTable();
		CommandReporting reporting = new CommandReporting(null, null, null);
		List<BSBlueprintString> bpStrings = BlueprintFinder.search(cfgBlueprint, reporting);
		reporting.getExceptions().forEach(e -> e.printStackTrace());
		Set<String> entities = new HashSet<>();
		Set<String> tiles = new HashSet<>();
		for (BSBlueprintString blueprintString : bpStrings) {
			for (BSBlueprint blueprint : blueprintString.findAllBlueprints()) {
				for (BSEntity entity : blueprint.entities) {
					if (entities.add(entity.name)) {
						System.out.println("CHECK ENTITY " + entity.name);
						table.getEntity(entity.name).get();
					}
				}
				for (BSTile tile : blueprint.tiles) {
					if (tiles.add(tile.name)) {
						System.out.println("CHECK TILE " + tile.name);
						table.getTile(tile.name).get();
					}
				}
			}
		}
		System.out.println();
		for (String entityName : entities.stream().sorted().collect(Collectors.toList())) {
			EntityPrototype proto = table.getEntity(entityName).get();
			String type = proto.lua().get("type").tojstring();
			StringBuilder sb = new StringBuilder();
			for (String part : type.split("-")) {
				sb.append(part.substring(0, 1).toUpperCase() + part.substring(1));
			}
			sb.append("Rendering");
			System.out.println("\t\t\"" + entityName + "\": \"" + sb.toString() + "\",");
		}
		System.out.println();
		for (String tileName : tiles.stream().sorted().collect(Collectors.toList())) {
			System.out.println("\t\t\"" + tileName + "\": false,");
		}

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

//		LuaValue luaVirtualSignal = table.getRaw("virtual-signal").get();
//		Utils.forEach(luaVirtualSignal, (k, v) -> {
//			System.out.println(k.tojstring() + " -- " + v.get("icon").tojstring());
//		});

//		table.getTiles().entrySet().stream()
//				.collect(Collectors.groupingBy(e -> e.getValue().lua().get("layer").checkint())).entrySet().stream()
//				.sorted(Comparator.comparing(e -> e.getKey())).forEach(e -> System.out.println(e.getKey() + ": "
//						+ e.getValue().stream().map(e2 -> e2.getKey()).collect(Collectors.joining(", ", "[", "]"))));
	}

}
