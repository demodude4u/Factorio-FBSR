package com.demod.fbsr;

import static com.demod.fbsr.Direction.*;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
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
import com.demod.factorio.fakelua.LuaValue;
import com.demod.factorio.prototype.TilePrototype;
import com.demod.fbsr.bs.BSPosition;
import com.demod.fbsr.bs.BSTile;
import com.demod.fbsr.fp.FPMaterialTextureParameters;
import com.demod.fbsr.fp.FPTileMainPictures;
import com.demod.fbsr.fp.FPTileSpriteLayoutVariant;
import com.demod.fbsr.fp.FPTileTransitionVariantLayout;
import com.demod.fbsr.fp.FPTileTransitions;
import com.demod.fbsr.fp.FPTileTransitionsVariants;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRect;
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

	public static final MapRect TILE_BOUNDS = MapRect.byUnit(0, 0, 1, 1);

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

	public interface TileRenderProcess {

		public abstract void initAtlas(Consumer<ImageDef> register);

		void tileCenter(Random rand, Consumer<MapRenderable> register, BSTile tile);

		void tileEdge(Random rand, Consumer<MapRenderable> register, MapPosition pos, List<TileEdgeRuleParam> params);
	}

	public class TileRenderProcessMain implements TileRenderProcess {
		private List<TileEdgeRuleParam> convertSidesToDoubleSides(List<TileEdgeRuleParam> params) {
			if (params.size() != 2) {
				return params;
			}
			for (TileEdgeRuleParam param : params) {
				if (!param.rule.equals("SIDE")) {
					return params;
				}
			}
			return ImmutableList.of(new TileEdgeRuleParam(params.stream().mapToInt(p -> p.variant).min().getAsInt(),
					null, fp -> fp.doubleSide));
		}

		// Uses main tiles and probabilities (bricks, platform, etc.)
		// TODO
		// Figure out how to work in probabilities and covering multiple tile sizes
		@Override
		public void tileCenter(Random rand, Consumer<MapRenderable> register, BSTile tile) {
			FPTileMainPictures main = protoVariantsMainSize1.get();

			int frame = rand.nextInt(main.count);
			register.accept(new MapSprite(new LayeredSpriteDef(main.defineImage(frame), Layer.DECALS, TILE_BOUNDS),
					tile.position.createPoint()));
		}

		@Override
		public void tileEdge(Random rand, Consumer<MapRenderable> register, MapPosition pos,
				List<TileEdgeRuleParam> params) {

			// TODO figure out why some tiles do not have an overlay!
			if (protoVariants.transition.get().overlayLayout.isPresent()) {
				FPTileTransitionVariantLayout overlay = protoVariants.transition.get().overlayLayout.get();
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

						int sourceWidth = (int) Math.round(64 / variant.scale);
						int sourceHeight = (int) Math.round(variant.tileHeight * 64 / variant.scale);

						register.accept(new MapSprite(new LayeredSpriteDef(variant., null, TILE_BOUNDS), pos))
						
						register.accept(new MapSprite(Layer.DECALS, data.getModImage(variant.spritesheet),
								new Rectangle(frame * sourceWidth, 0, sourceWidth, sourceHeight),
								MapRect.byUnit(pos, 1, variant.tileHeight)));
					}
				}
			}

			if (protoVariantsTransition.get().backgroundLayout.isPresent()) {
				FPTileTransitionVariantLayout background = protoVariantsTransition.get().backgroundLayout.get();
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

						int sourceWidth = (int) Math.round(64 / variant.scale);
						int sourceHeight = (int) Math.round(variant.tileHeight * 64 / variant.scale);

						register.accept(new MapSprite(Layer.UNDER_TILES, data.getModImage(variant.spritesheet),
								new Rectangle(frame * sourceWidth, param.variant * sourceHeight, sourceWidth,
										sourceHeight),
								MapRect.byUnit(pos, 1, variant.tileHeight)));
					}
				}
			}
		}

		@Override
		public void initAtlas(Consumer<ImageDef> register) {
			// TODO Auto-generated method stub

		}
	}

	public class TileRenderProcessMaterial implements TileRenderProcess {
		// Uses material_background and masks (concrete, etc.)
		// TODO
		// Create masking function to generate edge tiles
		@Override
		public void tileCenter(Random rand, Consumer<MapRenderable> register, BSTile tile) {
			// TODO shuffle the large tiles for variety (512x512 for example)

			FPMaterialTextureParameters material = protoVariantsMaterialBackground.get();

			int sourceSize = (int) Math.round(64 / material.scale);

			MapRect bounds = MapRect.byUnit(tile.position.x, tile.position.y, 1, 1);
			BufferedImage image = data.getModImage(material.picture);
			Rectangle source = new Rectangle(0, 0, sourceSize, sourceSize);
			int w = image.getWidth();
			int h = image.getHeight();
			MapPosition pos = tile.position.createPoint();
			source.x = ((int) Math.floor(pos.getX() * sourceSize) % w + w) % w;
			source.y = ((int) Math.floor(pos.getY() * sourceSize) % h + h) % h;

			register.accept(new MapSprite(Layer.DECALS, image, source, bounds));
		}

		@Override
		public void tileEdge(Random rand, Consumer<MapRenderable> register, MapPosition pos,
				List<TileEdgeRuleParam> params) {

			FPTileTransitions transitions = protoVariantsTransition.get();
			// TODO some tiles do not have overlay/mask!
//			FPTileTransitionVariantLayout overlay = transitions.overlayLayout.get();
//			FPTileTransitionVariantLayout mask = transitions.maskLayout.get();

			// TODO material edge
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

		class TileCell {
			int row, col;
			int layer;
			Optional<TileRendererFactory> mergeFactory;
			OptionalInt mergeLayer;
			BSTile tile;
			TileRendererFactory factory;
		}

		class TileEdgeCell {
			int row, col;
			int layer;
			TileRendererFactory factory;
			int adjCode = 0;
		}

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
				rand.setSeed(getRandomSeed(cell.row, cell.col, cell.layer, 0));
				cell.factory.renderProcess.tileCenter(rand, register, cell.tile);
			}

			// Render tile edges
			for (TileEdgeCell edgeCell : edgeCellLayers.get(layer)) {
				MapPosition pos = MapPosition.byUnit(edgeCell.col, edgeCell.row);
				List<TileEdgeRuleParam> params = tileRules.get(edgeCell.adjCode);
				rand.setSeed(getRandomSeed(edgeCell.row, edgeCell.col, edgeCell.layer, edgeCell.adjCode));

				// TODO detect double side scenario and do special rendering

				edgeCell.factory.renderProcess.tileEdge(rand, register, pos, params);
			}

			// Render tile blends (TODO)

		}

	}

	public static long getRandomSeed(int row, int col, int layer, int adjCode) {
		return ((row * 73856093) ^ (col * 19349663) ^ (layer * 83492791) ^ (adjCode * 123456789));
	}

	public void initAtlas(Consumer<ImageDef> register) {
		renderProcess.initAtlas(register);
	}

	public static void initFactories(List<TileRendererFactory> factories) {
		for (TileRendererFactory factory : factories) {
			factory.initFromPrototype(factory.getData().getTable());
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
	private Optional<FPMaterialTextureParameters> protoVariantsMaterialBackground;
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
		LuaValue luaVariants = prototype.lua().get("variants");
		protoVariants = new FPTileTransitionsVariants(prototype.lua().get("variants"));
		protoVariantsMainSize1 = protoVariants.main.stream().filter(fp -> fp.size == 1).findFirst();
		protoTransitionMergesWithTileID = FPUtils.optString(prototype.lua().get("transition_merges_with_tile"));
		protoTransitionMergesWithTile = protoTransitionMergesWithTileID
				.flatMap(k -> Optional.ofNullable(FactorioManager.lookupTileFactoryForName(k)));

		if (!protoVariants.main.isEmpty())
			renderProcess = new TileRenderProcessMain();
		else if (protoVariantsMaterialBackground.isPresent()) {
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
