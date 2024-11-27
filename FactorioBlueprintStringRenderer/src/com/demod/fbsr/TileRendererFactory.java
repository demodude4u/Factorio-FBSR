package com.demod.fbsr;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.prototype.TilePrototype;
import com.demod.fbsr.FBSR.TileRenderingTuple;
import com.demod.fbsr.bs.BSPosition;
import com.demod.fbsr.bs.BSTile;
import com.demod.fbsr.fp.FPMaterialTextureParameters;
import com.demod.fbsr.fp.FPTileSpriteLayout;
import com.demod.fbsr.fp.FPTileSpriteLayoutVariant;
import com.demod.fbsr.fp.FPTileTransitionVariantLayout;
import com.demod.fbsr.fp.FPTileTransitions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

public class TileRendererFactory {

	public static class FPTileMainPictures extends FPTileSpriteLayout {
		public final int size;
		public final double probability;
		public final Optional<List<Double>> weights;

		public FPTileMainPictures(LuaValue lua) {
			super(lua);
			size = lua.get("size").checkint();
			probability = lua.get("probability").optdouble(1.0);
			weights = FPUtils.optList(lua.get("weights"), LuaValue::todouble);
		}
	}

	public enum TileEdgeRule {
		SIDE(AC_N, AC_W | AC_E, fp -> fp.side), //
		OUTER_CORNER(AC_NE, AC_N | AC_E, fp -> fp.outerCorner), //
		U_TRANSITION(AC_W | AC_N | AC_E, AC_S, fp -> fp.uTransition), //
		O_TRANSITION(AC_W | AC_N | AC_E | AC_S, 0, fp -> fp.oTransition), //
		INNER_CORNER(AC_N | AC_E, AC_S | AC_W, fp -> fp.innerCorner),//
		;

		private final Function<FPTileTransitionVariantLayout, FPTileSpriteLayoutVariant> selector;
		private final int adjCodePresent;
		private final int adjCodeEmpty;

		private TileEdgeRule(int adjCodePresent, int adjCodeEmpty,
				Function<FPTileTransitionVariantLayout, FPTileSpriteLayoutVariant> selector) {
			this.adjCodePresent = adjCodePresent;
			this.adjCodeEmpty = adjCodeEmpty;
			this.selector = selector;
		}

		public Function<FPTileTransitionVariantLayout, FPTileSpriteLayoutVariant> getSelector() {
			return selector;
		}
	}

	public static class TileEdgeRuleParam {
		private final int variant;
		private final TileEdgeRule rule;

		public TileEdgeRuleParam(int variant, TileEdgeRule rule) {
			this.variant = variant;
			this.rule = rule;
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

		public TileEdgeRule getRule() {
			return rule;
		}

		public int getVariant() {
			return variant;
		}

		@Override
		public int hashCode() {
			return Objects.hash(rule, variant);
		}
	}

	public static abstract class TileRenderProcess {
		// TODO
		public abstract void tileCenter(Random rand, Consumer<Renderer> register, BSTile tile);

		public abstract void tileEdge(Random rand, Consumer<Renderer> register, Point2D.Double pos,
				TileEdgeRuleParam param);
	}

	public class TileRenderProcessMain extends TileRenderProcess {
		// Uses main tiles and probabilities (bricks, platform, etc.)
		// TODO
		// Figure out how to work in probabilities and covering multiple tile sizes
		@Override
		public void tileCenter(Random rand, Consumer<Renderer> register, BSTile tile) {
			FPTileMainPictures main = protoVariantsMainSize1.get();

			int frame = rand.nextInt(main.count);

			int sourceSize = (int) Math.round(main.size * 32 / main.scale);

			Sprite sprite = new Sprite();
			sprite.bounds = new Rectangle2D.Double(0, 0, main.size, main.size);
			sprite.image = FactorioData.getModImage(main.picture);
			sprite.source = new Rectangle(0, 0, sourceSize, sourceSize);
			sprite.source.x = frame * sprite.source.width;
			register.accept(RenderUtils.spriteRenderer(Layer.DECALS, sprite, tile));
		}

		@Override
		public void tileEdge(Random rand, Consumer<Renderer> register, Point2D.Double pos, TileEdgeRuleParam param) {
			// TODO main edge
			// TODO main background edge (space platforms)
		}
	}

	public class TileRenderProcessMaterial extends TileRenderProcess {
		// Uses material_background and masks (concrete, etc.)
		// TODO
		// Create masking function to generate edge tiles
		@Override
		public void tileCenter(Random rand, Consumer<Renderer> register, BSTile tile) {
			// TODO shuffle the large tiles for variety (512x512 for example)

			FPMaterialTextureParameters material = protoVariantsMaterialBackground.get();

			int sourceSize = (int) Math.round(32 / material.scale);

			// TODO slide x and y across the source image accordingly

			Sprite sprite = new Sprite();
			sprite.bounds = new Rectangle2D.Double(0, 0, 1, 1);
			sprite.image = FactorioData.getModImage(main.picture);
			sprite.source = new Rectangle(0, 0, sourceSize, sourceSize);
			sprite.source.x = frame * sprite.source.width;
			register.accept(RenderUtils.spriteRenderer(Layer.DECALS, sprite, tile));
		}

		@Override
		public void tileEdge(Random rand, Consumer<Renderer> register, Point2D.Double pos, TileEdgeRuleParam param) {
			// TODO material edge
		}
	}

	public static List<List<TileEdgeRuleParam>> tileRules = new ArrayList<>();
	static {
		IntStream.range(0, 0xFF).forEach(i -> tileRules.add(new ArrayList<>()));
		for (TileEdgeRule rule : TileEdgeRule.values()) {
			int adjCodePresent = rule.adjCodePresent;
			int adjCodeEmpty = rule.adjCodeEmpty;
			for (int variant = 0; variant < 4; variant++) {
				for (int adjCode = 0; adjCode < 0xFF; adjCode++) {
					int adjCodeCheck = (adjCode | adjCodePresent) & ~adjCodeEmpty;
					if (adjCode == adjCodeCheck) {
						TileEdgeRuleParam param = new TileEdgeRuleParam(variant, rule);
						List<TileEdgeRuleParam> adjRules = tileRules.get(adjCode);
						if (!adjRules.contains(param)) {
							adjRules.add(param);
						}
					}
				}
				// rotate 90 degrees
				adjCodePresent = ((adjCodePresent << 6) | (adjCodePresent >> 2)) & 0xFF;
				adjCodeEmpty = ((adjCodeEmpty << 6) | (adjCodeEmpty >> 2)) & 0xFF;
			}
		}
	}

	// Adjcodes for edge rules
	public static final int AC_N = 1 << Direction.NORTH.ordinal();
	public static final int AC_NE = 1 << Direction.NORTHEAST.ordinal();
	public static final int AC_E = 1 << Direction.EAST.ordinal();
	public static final int AC_SE = 1 << Direction.SOUTHEAST.ordinal();
	public static final int AC_S = 1 << Direction.SOUTH.ordinal();
	public static final int AC_SW = 1 << Direction.SOUTHWEST.ordinal();
	public static final int AC_W = 1 << Direction.WEST.ordinal();
	public static final int AC_NW = 1 << Direction.NORTHWEST.ordinal();

	public static final TileRendererFactory UNKNOWN = new TileRendererFactory() {
		Set<String> labeledTypes = new HashSet<>();

		@Override
		public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BSTile tile) {
			Point2D.Double pos = tile.position.createPoint();
			Rectangle2D.Double bounds = new Rectangle2D.Double(pos.x + 0.25, pos.y + 0.25, 0.5, 0.5);
			float randomFactor = new Random(tile.name.hashCode()).nextFloat();
			register.accept(new Renderer(Layer.ABOVE_TILES, bounds) {
				@Override
				public void render(Graphics2D g) {
					g.setColor(RenderUtils.withAlpha(Color.getHSBColor(randomFactor, 0.6f, 0.4f), 128));
					g.fill(new Ellipse2D.Double(bounds.x, bounds.y, bounds.width, bounds.height));
					g.setColor(Color.gray);
					g.setFont(new Font("Monospaced", Font.BOLD, 1).deriveFont(0.5f));
					g.drawString("?", (float) bounds.getCenterX() - 0.125f, (float) bounds.getCenterY() + 0.15f);
				}
			});
			register.accept(new Renderer(Layer.ENTITY_INFO_TEXT, bounds) {
				@Override
				public void render(Graphics2D g) {
					if (labeledTypes.add(tile.name)) {
						g.setFont(g.getFont().deriveFont(0.4f));
						float textX = (float) bounds.x;
						float textY = (float) (bounds.y + bounds.height * randomFactor);
						g.setColor(Color.darkGray);
						g.drawString(tile.name, textX + 0.05f, textY + 0.05f);
						g.setColor(Color.white);
						g.drawString(tile.name, textX, textY);
					}
				}
			});
		}

		@Override
		public void populateWorldMap(WorldMap map, DataTable dataTable, BSTile tile) {
			if (!labeledTypes.isEmpty()) {
				labeledTypes.clear();
			}
		}
	};

	private static Map<String, TileRendererFactory> byName = new HashMap<>();

	static {
		byName.put("acid-refined-concrete", new TileRendererFactory());
		byName.put("black-refined-concrete", new TileRendererFactory());
		byName.put("blue-refined-concrete", new TileRendererFactory());
		byName.put("brown-refined-concrete", new TileRendererFactory());
		byName.put("concrete", new TileRendererFactory());
		byName.put("cyan-refined-concrete", new TileRendererFactory());
		byName.put("green-refined-concrete", new TileRendererFactory());
		byName.put("hazard-concrete-left", new TileRendererFactory());
		byName.put("hazard-concrete-right", new TileRendererFactory());
		byName.put("lab-dark-1", new TileRendererFactory());
		byName.put("lab-dark-2", new TileRendererFactory());
		byName.put("lab-white", new TileRendererFactory());
		byName.put("landfill", new TileRendererFactory());
		byName.put("orange-refined-concrete", new TileRendererFactory());
		byName.put("pink-refined-concrete", new TileRendererFactory());
		byName.put("purple-refined-concrete", new TileRendererFactory());
		byName.put("red-refined-concrete", new TileRendererFactory());
		byName.put("refined-concrete", new TileRendererFactory());
		byName.put("refined-hazard-concrete-left", new TileRendererFactory());
		byName.put("refined-hazard-concrete-right", new TileRendererFactory());
		byName.put("space-platform-foundation", new TileRendererFactory());
		byName.put("stone-path", new TileRendererFactory());
		byName.put("yellow-refined-concrete", new TileRendererFactory());
	}

	private static boolean prototypesInitialized = false;

	public static void createAllRenderers(Consumer<Renderer> register, List<TileRenderingTuple> tiles) {

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
		Consumer<TileCell> randSeedCell = c -> rand
				.setSeed(new Random((c.row << 32) | (c.col << 16) | c.layer).nextLong());
		Consumer<TileEdgeCell> randSeedEdgeCell = c -> rand
				.setSeed(new Random((c.row << 48) | (c.col << 32) | (c.layer << 16) | c.adjCode).nextLong());

		// <row, col, cell>
		Table<Integer, Integer, TileCell> tileMap = HashBasedTable.create();

		// XXX should I also do render order (left to right, top to bottom)?
		List<TileRenderingTuple> tileOrder = tiles.stream().sorted(Comparator.comparing(t -> t.factory.protoLayer))
				.collect(Collectors.toList());

		// <layer, <row, col, cell>>
		LinkedHashMap<Integer, Table<Integer, Integer, TileEdgeCell>> tileEdgeMaps = new LinkedHashMap<>();

		TreeSet<Integer> activeLayers = new TreeSet<>();
		Multimap<Integer, TileCell> cellLayers = ArrayListMultimap.create();
		Multimap<Integer, TileEdgeCell> edgeCellLayers = ArrayListMultimap.create();

		// Populate tile map
		for (TileRenderingTuple tuple : tileOrder) {
			TileCell cell = new TileCell();
			cell.row = (int) tuple.tile.position.y;
			cell.col = (int) tuple.tile.position.x;
			cell.layer = tuple.factory.protoLayer;
			cell.mergeFactory = tuple.factory.protoTransitionMergesWithTile;
			cell.mergeLayer = cell.mergeFactory.map(f -> OptionalInt.of(f.protoLayer)).orElse(OptionalInt.empty());
			cell.tile = tuple.tile;
			cell.factory = tuple.factory;
			tileMap.put(cell.row, cell.col, cell);
			activeLayers.add(cell.layer);
			cellLayers.put(cell.layer, cell);
		}

		// Populate edge maps
		for (TileRenderingTuple tuple : tileOrder) {
			BSPosition pos = tuple.tile.position;
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
				if (adjCell.layer >= cell.layer) {
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
				randSeedCell.accept(cell);
				cell.factory.renderProcess.tileCenter(rand, register, cell.tile);
			}

			// Render tile edges
			for (TileEdgeCell edgeCell : edgeCellLayers.get(layer)) {
				Point2D.Double pos = new Point2D.Double(edgeCell.col, edgeCell.row);
				List<TileEdgeRuleParam> params = tileRules.get(edgeCell.adjCode);
				randSeedEdgeCell.accept(edgeCell);
				for (TileEdgeRuleParam param : params) {
					edgeCell.factory.renderProcess.tileEdge(rand, register, pos, param);
				}
			}

			// Render tile blends (TODO)

		}

	}

	public static TileRendererFactory forName(String name) {
		return Optional.ofNullable(byName.get(name)).orElse(UNKNOWN);
	}

	public static void initPrototypes(DataTable table) {
		if (prototypesInitialized) {
			return;
		}
		for (Entry<String, TileRendererFactory> entry : byName.entrySet()) {
			System.out.println("Initializing " + entry.getKey());
			TilePrototype prototype = table.getTile(entry.getKey()).get();
			entry.getValue().setPrototype(prototype);
			entry.getValue().initFromPrototype(table, prototype);
		}
		prototypesInitialized = true;
	}

	protected TilePrototype prototype;
	private List<FPTileMainPictures> protoVariantsMain;
	private Optional<FPTileMainPictures> protoVariantsMainSize1;
	private Optional<FPTileTransitions> protoVariantsTransition;
	private Optional<FPMaterialTextureParameters> protoVariantsMaterialBackground;

	private int protoLayer;

	private TileRenderProcess renderProcess = null;
	private Optional<String> protoTransitionMergesWithTileID;
	private Optional<TileRendererFactory> protoTransitionMergesWithTile;

	// TODO fix UNKNOWN so we don't need this
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BSTile tile) {
	}

	public TilePrototype getPrototype() {
		return prototype;
	}

	public void initFromPrototype(DataTable table, TilePrototype prototype) {
		protoLayer = prototype.lua().get("layer").checkint();
		LuaValue luaVariants = prototype.lua().get("variants");
		protoVariantsMain = FPUtils.list(luaVariants.get("main"), FPTileMainPictures::new);
		protoVariantsMainSize1 = protoVariantsMain.stream().filter(fp -> fp.size == 1).findFirst();
		protoVariantsTransition = FPUtils.opt(luaVariants.get("transition"), FPTileTransitions::new);
		protoVariantsMaterialBackground = FPUtils.opt(luaVariants.get("material_background"),
				FPMaterialTextureParameters::new);
		protoTransitionMergesWithTileID = FPUtils.optString(prototype.lua().get("transition_merges_with_tile"));
		protoTransitionMergesWithTile = protoTransitionMergesWithTileID
				.flatMap(k -> Optional.ofNullable(byName.get(k)));

		if (!protoVariantsMain.isEmpty())
			renderProcess = new TileRenderProcessMain();
		else if (protoVariantsMaterialBackground.isPresent()) {
			renderProcess = new TileRenderProcessMaterial();
		}
	}

	// TODO fix UNKNOWN so we don't need this
	public void populateWorldMap(WorldMap map, DataTable dataTable, BSTile tile) {
	}

	public void setPrototype(TilePrototype prototype) {
		this.prototype = prototype;
	}
}
