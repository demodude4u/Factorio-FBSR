package com.demod.fbsr;

import static com.demod.fbsr.Direction.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.prototype.TilePrototype;
import com.demod.fbsr.bs.BSPosition;
import com.demod.fbsr.bs.BSTile;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.LayeredSpriteDef;
import com.demod.fbsr.def.MaterialDef;
import com.demod.fbsr.fp.FPMaterialTextureParameters;
import com.demod.fbsr.fp.FPTileMainPictures;
import com.demod.fbsr.fp.FPTileSpriteLayoutVariant;
import com.demod.fbsr.fp.FPTileTransitionVariantLayout;
import com.demod.fbsr.fp.FPTileTransitions;
import com.demod.fbsr.fp.FPTileTransitionsVariants;
import com.demod.fbsr.map.MapMaterialMaskedTile;
import com.demod.fbsr.map.MapMaterialTile;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;
import com.demod.fbsr.map.MapSprite;
import com.demod.fbsr.map.MapTile;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

public class TileRendererFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(TileRendererFactory.class);

	public enum TileEdgeRule {
		SIDE(NORTH.adjCode(), WEST.adjCode() | EAST.adjCode(), fp -> fp.side), //
		OUTER_CORNER(NORTHEAST.adjCode(), NORTH.adjCode() | EAST.adjCode(), fp -> fp.outerCorner), //
		U_TRANSITION(WEST.adjCode() | NORTH.adjCode() | EAST.adjCode(), SOUTH.adjCode(), fp -> fp.uTransition), //
		O_TRANSITION(WEST.adjCode() | NORTH.adjCode() | EAST.adjCode() | SOUTH.adjCode(), 0, fp -> fp.oTransition), //
		INNER_CORNER(NORTH.adjCode() | EAST.adjCode(), SOUTH.adjCode() | WEST.adjCode(), fp -> fp.innerCorner),//
		;

		private final int adjCodePresent;
		private final int adjCodeEmpty;
		private final Function<FPTileTransitionVariantLayout, Optional<FPTileSpriteLayoutVariant>> selector;

		private TileEdgeRule(int adjCodePresent, int adjCodeEmpty,
				Function<FPTileTransitionVariantLayout, Optional<FPTileSpriteLayoutVariant>> selector) {
			this.adjCodePresent = adjCodePresent;
			this.adjCodeEmpty = adjCodeEmpty;
			this.selector = selector;
		}

		public Function<FPTileTransitionVariantLayout, Optional<FPTileSpriteLayoutVariant>> getSelector() {
			return selector;
		}
	}

	public static class TileEdgeRuleParam {
		private final int variant;
		private final String rule;
		private final Function<FPTileTransitionVariantLayout, Optional<FPTileSpriteLayoutVariant>> selector;

		public TileEdgeRuleParam(int variant, String rule,
				Function<FPTileTransitionVariantLayout, Optional<FPTileSpriteLayoutVariant>> selector) {
			this.variant = variant;
			this.rule = rule;
			this.selector = selector;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TileEdgeRuleParam other = (TileEdgeRuleParam) obj;
			return rule == other.rule && variant == other.variant;
		}

		public Function<FPTileTransitionVariantLayout, Optional<FPTileSpriteLayoutVariant>> getSelector() {
			return selector;
		}

		public int getVariant() {
			return variant;
		}

		@Override
		public int hashCode() {
			return Objects.hash(rule, variant);
		}
	}

	private static class TileCell {
		int row, col;
		int layer;
		Optional<TileRendererFactory> mergeFactory;
		OptionalInt mergeLayer;
		BSTile tile;
		TileRendererFactory factory;
	}

	private static class TileEdgeCell {
		int row, col;
		int layer;
		TileRendererFactory factory;
		int adjCode = 0;
	}

	public interface TileRenderProcess {
		void tileCenter(Random rand, Consumer<MapRenderable> register, TileCell cell);

		void tileEdge(Random rand, Consumer<MapRenderable> register, TileEdgeCell cell, MapPosition pos,
				List<TileEdgeRuleParam> params);

		void initAtlas(Consumer<ImageDef> register);
	}

	public class TileRenderProcessMain implements TileRenderProcess {

		// Uses main tiles and probabilities (bricks, platform, etc.)
		// TODO
		// Figure out how to work in probabilities and covering multiple tile sizes
		@Override
		public void tileCenter(Random rand, Consumer<MapRenderable> register, TileCell cell) {

			FPTileMainPictures main = protoVariantsMainSize1.get();

			rand.setSeed(getRandomSeed(cell.row, cell.col, cell.layer, 0));
			int frame = rand.nextInt(main.getLimitedCount());

			register.accept(new MapSprite(new LayeredSpriteDef(main.defineImage(frame), Layer.DECALS),
					cell.tile.position.createPoint()));
		}

		@Override
		public void tileEdge(Random rand, Consumer<MapRenderable> register, TileEdgeCell cell, MapPosition pos,
				List<TileEdgeRuleParam> params) {
			FPTileTransitions transitions = protoVariants.transition.get();

			rand.setSeed(getRandomSeed(cell.row, cell.col, cell.layer, cell.adjCode));

			// TODO figure out why some tiles do not have an overlay!
			if (transitions.overlayLayout.isPresent()) {
				FPTileTransitionVariantLayout overlay = transitions.overlayLayout.get();
				List<TileEdgeRuleParam> overlayParams;
				if (overlay.doubleSide.isPresent()) {
					overlayParams = convertSidesToDoubleSides(params);
				} else {
					overlayParams = params;
				}
				for (TileEdgeRuleParam param : overlayParams) {
					Optional<FPTileSpriteLayoutVariant> optVariant = param.getSelector().apply(overlay);
					if (optVariant.isPresent()) {
						FPTileSpriteLayoutVariant variant = optVariant.get();

						int frame = rand.nextInt(variant.count);

						register.accept(new MapSprite(variant.defineImage(param.variant, frame), Layer.DECALS, pos));
					}
				}
			}

			if (transitions.backgroundLayout.isPresent()) {
				FPTileTransitionVariantLayout background = transitions.backgroundLayout.get();
				List<TileEdgeRuleParam> backgroundParams;
				if (background.doubleSide.isPresent()) {
					backgroundParams = convertSidesToDoubleSides(params);
				} else {
					backgroundParams = params;
				}
				for (TileEdgeRuleParam param : backgroundParams) {
					Optional<FPTileSpriteLayoutVariant> optVariant = param.getSelector().apply(background);
					if (optVariant.isPresent()) {
						FPTileSpriteLayoutVariant variant = optVariant.get();

						int frame = rand.nextInt(variant.count);

						register.accept(
								new MapSprite(variant.defineImage(param.variant, frame), Layer.UNDER_TILES, pos));
					}
				}
			}
		}

		@Override
		public void initAtlas(Consumer<ImageDef> register) {
			protoVariantsMainSize1.ifPresent(fp -> fp.getDefs(register));
			protoVariants.transition.get().overlayLayout.ifPresent(fp -> fp.getDefs(register));
			protoVariants.transition.get().backgroundLayout.ifPresent(fp -> fp.getDefs(register));
		}
	}

	public class TileRenderProcessMaterial implements TileRenderProcess {
		// Uses material_background and masks (concrete, etc.)
		// TODO
		// Create masking function to generate edge tiles
		@Override
		public void tileCenter(Random rand, Consumer<MapRenderable> register, TileCell cell) {
			BSTile tile = cell.tile;

			FPMaterialTextureParameters material = protoVariants.materialBackground.get();
			int tw = material.getTexWidthTiles();
			int th = material.getTexHeightTiles();

			rand.setSeed(getRandomSeed(cell.row / th, cell.col / tw, cell.layer, 0));
			int frame = rand.nextInt(material.getLimitedCount());

			register.accept(new MapMaterialTile(material.defineMaterial(frame), cell.row % th, cell.col % tw,
					tile.position.createPoint()));
		}

		@Override
		public void tileEdge(Random rand, Consumer<MapRenderable> register, TileEdgeCell cell, MapPosition pos,
				List<TileEdgeRuleParam> params) {

			FPMaterialTextureParameters material = protoVariants.materialBackground.get();
			int tw = material.getTexWidthTiles();
			int th = material.getTexHeightTiles();

			rand.setSeed(getRandomSeed(cell.row / th, cell.col / tw, cell.layer, 0));
			int materialFrame = rand.nextInt(material.getLimitedCount());
			MaterialDef materialDef = material.defineMaterial(materialFrame);

			FPTileTransitions transitions = protoVariants.transition.get();
			FPTileTransitionVariantLayout overlay = transitions.overlayLayout.get();
			FPTileTransitionVariantLayout mask = transitions.maskLayout.get();

			rand.setSeed(getRandomSeed(cell.row, cell.col, cell.layer, cell.adjCode));

			if (overlay.doubleSide.isPresent()) {
				params = convertSidesToDoubleSides(params);
			}

			for (TileEdgeRuleParam param : params) {
				Optional<FPTileSpriteLayoutVariant> optVariantMask = param.getSelector().apply(mask);
				Optional<FPTileSpriteLayoutVariant> optVariantOverlay = param.getSelector().apply(overlay);
				if (optVariantMask.isPresent() && optVariantOverlay.isPresent()) {
					FPTileSpriteLayoutVariant variantMask = optVariantMask.get();
					FPTileSpriteLayoutVariant variantOverlay = optVariantOverlay.get();

					if (variantMask.count != variantOverlay.count) {
						throw new IllegalStateException("Mask and overlay do not match!");
					}

					int frame = rand.nextInt(variantMask.count);

					register.accept(new MapMaterialMaskedTile(materialDef,
							variantMask.defineImage(param.variant, frame), cell.row % th, cell.col % tw, pos));
					register.accept(new MapSprite(variantOverlay.defineImage(param.variant, frame), Layer.DECALS, pos));
				}
			}

			rand.setSeed(getRandomSeed(cell.row, cell.col, cell.layer, cell.adjCode));
		}

		@Override
		public void initAtlas(Consumer<ImageDef> register) {
			protoVariants.materialBackground.get().getDefs().forEach(register);
			protoVariants.transition.ifPresent(fp -> fp.overlayLayout.ifPresent(fp2 -> fp2.getDefs(register)));
			protoVariants.transition.ifPresent(fp -> fp.maskLayout.ifPresent(fp2 -> fp2.getDefs(register)));
			// TODO edge tiles
		}
	}

	public static List<List<TileEdgeRuleParam>> tileRules = new ArrayList<>();

	static {
		IntStream.range(0, 256).forEach(i -> tileRules.add(new ArrayList<>()));
		for (TileEdgeRule rule : TileEdgeRule.values()) {
			int adjCodePresent = rule.adjCodePresent;
			int adjCodeEmpty = rule.adjCodeEmpty;
			for (int variant = 0; variant < 4; variant++) {
				for (int adjCode = 0; adjCode < 0xFF; adjCode++) {
					int adjCodeCheck = (adjCode | adjCodePresent) & ~adjCodeEmpty;
					if (adjCode == adjCodeCheck) {
						TileEdgeRuleParam param = new TileEdgeRuleParam(variant, rule.name(), rule.selector);
						List<TileEdgeRuleParam> adjRules = tileRules.get(adjCode);
						if (!adjRules.contains(param)) {
							adjRules.add(param);
						}
					}
				}
				// rotate 90 degrees
				int nextAdjCodePresent = ((adjCodePresent << 2) | (adjCodePresent >> 6)) & 0xFF;
				int nextAdjCodeEmpty = ((adjCodeEmpty << 2) | (adjCodeEmpty >> 6)) & 0xFF;
				if (nextAdjCodePresent == adjCodePresent && nextAdjCodeEmpty == adjCodeEmpty) {
					break;
				}
				adjCodePresent = nextAdjCodePresent;
				adjCodeEmpty = nextAdjCodeEmpty;
			}
		}
		for (int adjCode = 0; adjCode < 256; adjCode++) {
			List<TileEdgeRuleParam> params = tileRules.get(adjCode);
			if (params.isEmpty()) {
				continue;
			}
//			System.out.println("RULE " + Integer.toBinaryString((1 << 15) | adjCode).substring(8, 16) + " -- " + params
//					.stream().map(p -> p.rule.name() + "_" + p.variant).collect(Collectors.joining(", ", "[", "]")));
		}
	}

	public static void createAllRenderers(Consumer<MapRenderable> register, List<MapTile> tiles) {

		// TODO how do I decide which edge factory for matching layers? (example,
		// hazard-concrete-left/right)

		// XXX this is terrible
		// TODO make a predictable random method consistent for every coordinate
		Random rand = new Random();

		// <row, col, cell>
		Table<Integer, Integer, TileCell> tileMap = HashBasedTable.create();

		// XXX should I also do render order (left to right, top to bottom)?
		List<MapTile> tileOrder = tiles.stream().filter(t -> !t.getFactory().isUnknown())
				.sorted(Comparator.comparing(t -> t.getFactory().protoLayer)).collect(Collectors.toList());

		// <layer, <row, col, cell>>
		LinkedHashMap<Integer, Table<Integer, Integer, TileEdgeCell>> tileEdgeMaps = new LinkedHashMap<>();

		TreeSet<Integer> activeLayers = new TreeSet<>();
		Multimap<Integer, TileCell> cellLayers = ArrayListMultimap.create();
		Multimap<Integer, TileEdgeCell> edgeCellLayers = ArrayListMultimap.create();

		// Populate tile map
		for (MapTile mapTile : tileOrder) {
			TileCell cell = new TileCell();
			BSPosition pos = mapTile.fromBlueprint().position;
			cell.row = (int) pos.y;
			cell.col = (int) pos.x;
			cell.layer = mapTile.getFactory().protoLayer;
			cell.mergeFactory = mapTile.getFactory().protoTransitionMergesWithTile;
			cell.mergeLayer = cell.mergeFactory.map(f -> OptionalInt.of(f.protoLayer)).orElse(OptionalInt.empty());
			cell.tile = mapTile.fromBlueprint();
			cell.factory = mapTile.getFactory();
			tileMap.put(cell.row, cell.col, cell);
			activeLayers.add(cell.layer);
			cellLayers.put(cell.layer, cell);
		}

		// Populate edge maps
		for (MapTile mapTile : tileOrder) {
			BSPosition pos = mapTile.fromBlueprint().position;
			int row = (int) pos.y;
			int col = (int) pos.x;
			TileCell cell = tileMap.get(row, col);

			Table<Integer, Integer, TileEdgeCell> edgeMap = tileEdgeMaps.get(cell.layer);
			if (edgeMap == null) {
				tileEdgeMaps.put(cell.layer, edgeMap = HashBasedTable.create());
			}

			for (Direction direction : Direction.values()) {
				int adjRow = row + direction.getDy();
				int adjCol = col + direction.getDx();

				TileCell adjCell = tileMap.get(adjRow, adjCol);
				if (adjCell != null && adjCell.layer >= cell.layer) {
					continue;
				}

				TileEdgeCell edgeCell = edgeMap.get(adjRow, adjCol);
				if (edgeCell == null) {
					edgeCell = new TileEdgeCell();
					edgeCell.row = adjRow;
					edgeCell.col = adjCol;
					edgeCell.layer = cell.layer;
					edgeCell.factory = cell.factory;
					edgeMap.put(adjRow, adjCol, edgeCell);
					activeLayers.add(edgeCell.layer);
					edgeCellLayers.put(edgeCell.layer, edgeCell);
				}
				edgeCell.adjCode |= (1 << direction.back().ordinal());
			}
		}

		for (int layer : activeLayers) {

			// Render tile centers
			for (TileCell cell : cellLayers.get(layer)) {
				cell.factory.renderProcess.tileCenter(rand, register, cell);
			}

			// Render tile edges
			for (TileEdgeCell cell : edgeCellLayers.get(layer)) {
				MapPosition pos = MapPosition.byUnit(cell.col, cell.row);
				List<TileEdgeRuleParam> params = tileRules.get(cell.adjCode);

				// TODO detect double side scenario and do special rendering

				cell.factory.renderProcess.tileEdge(rand, register, cell, pos, params);
			}

			// Render tile blends (TODO)

		}

	}

	private static List<TileEdgeRuleParam> convertSidesToDoubleSides(List<TileEdgeRuleParam> params) {
		if (params.size() != 2) {
			return params;
		}
		// TODO strings bad
		for (TileEdgeRuleParam param : params) {
			if (!param.rule.equals("SIDE")) {
				return params;
			}
		}
		return ImmutableList.of(new TileEdgeRuleParam(params.stream().mapToInt(p -> p.variant).min().getAsInt(), null,
				fp -> fp.doubleSide));
	}

	public static long getRandomSeed(int row, int col, int layer, int adjCode) {
		return ((row * 73856093) ^ (col * 19349663) ^ (layer * 83492791) ^ (adjCode * 123456789));
	}

	public void initAtlas(Consumer<ImageDef> register) {
		renderProcess.initAtlas(register);
	}

	public static void initFactories(List<TileRendererFactory> factories) {
		for (TileRendererFactory factory : factories) {
			try {
				factory.initFromPrototype(factory.getData().getTable());
				factory.initAtlas(AtlasManager::registerDef);
			} catch (Exception e) {
				LOGGER.error("TILE {}", factory.getName());
				throw e;
			}
		}
		LOGGER.info("Initialized {} tiles.", factories.size());
	}

	public static void registerFactories(Consumer<TileRendererFactory> register, FactorioData data, JSONObject json) {
		DataTable table = data.getTable();
		for (String groupName : json.keySet().stream().sorted().collect(Collectors.toList())) {
			JSONArray jsonGroup = json.getJSONArray(groupName);
			for (int i = 0; i < jsonGroup.length(); i++) {
				String tileName = jsonGroup.getString(i);
				TilePrototype prototype = table.getTile(tileName).get();
				TileRendererFactory factory = new TileRendererFactory();
				factory.setName(tileName);
				factory.setGroupName(groupName);
				factory.setData(data);
				factory.setPrototype(prototype);
				register.accept(factory);
			}
		}
	}

	protected String name;
	protected String groupName;
	protected FactorioData data;
	protected TilePrototype prototype;

	private FPTileTransitionsVariants protoVariants;
	private Optional<FPTileMainPictures> protoVariantsMainSize1;
	private int protoLayer;
	private Optional<String> protoTransitionMergesWithTileID;
	private Optional<TileRendererFactory> protoTransitionMergesWithTile;

	private TileRenderProcess renderProcess = null;

	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapTile tile) {
	}

	public FactorioData getData() {
		return data;
	}

	public String getGroupName() {
		return groupName;
	}

	public String getName() {
		return name;
	}

	public TilePrototype getPrototype() {
		return prototype;
	}

	public void initFromPrototype(DataTable table) {
		protoLayer = prototype.lua().get("layer").checkint();
		protoVariants = new FPTileTransitionsVariants(prototype.lua().get("variants"), 10);
		protoVariantsMainSize1 = protoVariants.main.stream().filter(fp -> fp.size == 1).findFirst();
		protoTransitionMergesWithTileID = FPUtils.optString(prototype.lua().get("transition_merges_with_tile"));
		protoTransitionMergesWithTile = protoTransitionMergesWithTileID
				.flatMap(k -> Optional.ofNullable(FactorioManager.lookupTileFactoryForName(k)));

		if (!protoVariants.main.isEmpty())
			renderProcess = new TileRenderProcessMain();
		else if (protoVariants.materialBackground.isPresent()) {
			renderProcess = new TileRenderProcessMaterial();
		}
	}

	public boolean isUnknown() {
		return false;
	}

	// TODO fix UNKNOWN so we don't need this
	public void populateWorldMap(WorldMap map, MapTile tile) {
	}

	public void setData(FactorioData data) {
		this.data = data;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPrototype(TilePrototype prototype) {
		this.prototype = prototype;
	}
}
