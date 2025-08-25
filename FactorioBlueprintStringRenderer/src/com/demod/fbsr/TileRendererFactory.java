package com.demod.fbsr;

import static com.demod.fbsr.Direction.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.xml.transform.TransformerFactory;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.prototype.TilePrototype;
import com.demod.fbsr.Profile.ManifestModInfo;
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

	public enum TileEdgeRuleMode {
		NO_DOUBLE, WITH_DOUBLE, MERGED_UO
	}

	public enum TileEdgeRule {
		SIDE(NORTH.adjCode(), WEST.adjCode() | EAST.adjCode(), fp -> fp.side, 4, TileEdgeRuleMode.NO_DOUBLE,
				TileEdgeRuleMode.MERGED_UO), //
		SIDE_NO_FAR(NORTH.adjCode(), WEST.adjCode() | EAST.adjCode() | SOUTH.adjCode(), fp -> fp.side, 4,
				TileEdgeRuleMode.WITH_DOUBLE), //
		DOUBLE_SIDE(NORTH.adjCode() | SOUTH.adjCode(), WEST.adjCode() | EAST.adjCode(), fp -> fp.doubleSide, 2,
				TileEdgeRuleMode.WITH_DOUBLE), //
		OUTER_CORNER(NORTHEAST.adjCode(), NORTH.adjCode() | EAST.adjCode(), fp -> fp.outerCorner, 4,
				TileEdgeRuleMode.NO_DOUBLE, TileEdgeRuleMode.WITH_DOUBLE, TileEdgeRuleMode.MERGED_UO), //
		U_TRANSITION(WEST.adjCode() | NORTH.adjCode() | EAST.adjCode(), SOUTH.adjCode(), fp -> fp.uTransition, 4,
				TileEdgeRuleMode.NO_DOUBLE, TileEdgeRuleMode.WITH_DOUBLE, TileEdgeRuleMode.MERGED_UO), //
		O_TRANSITION(WEST.adjCode() | NORTH.adjCode() | EAST.adjCode() | SOUTH.adjCode(), 0, fp -> fp.oTransition, 1,
				TileEdgeRuleMode.NO_DOUBLE, TileEdgeRuleMode.WITH_DOUBLE, TileEdgeRuleMode.MERGED_UO), //
		INNER_CORNER(NORTH.adjCode() | EAST.adjCode(), SOUTH.adjCode() | WEST.adjCode(), fp -> fp.innerCorner, 4,
				TileEdgeRuleMode.NO_DOUBLE, TileEdgeRuleMode.WITH_DOUBLE), //
		INNER_CORNER_MERGE_UO_1(NORTH.adjCode() | EAST.adjCode(), SOUTH.adjCode() | WEST.adjCode(), fp -> fp.side, 4,
				TileEdgeRuleMode.MERGED_UO), //
		INNER_CORNER_MERGE_UO_2(NORTH.adjCode() | WEST.adjCode(), SOUTH.adjCode() | EAST.adjCode(), fp -> fp.side, 4,
				TileEdgeRuleMode.MERGED_UO),//
		;

		private final int adjCodePresent;
		private final int adjCodeEmpty;
		private final Function<FPTileTransitionVariantLayout, Optional<FPTileSpriteLayoutVariant>> selector;
		private final int variants;
		private final List<TileEdgeRuleMode> modes;

		private TileEdgeRule(int adjCodePresent, int adjCodeEmpty,
				Function<FPTileTransitionVariantLayout, Optional<FPTileSpriteLayoutVariant>> selector, int variants,
				TileEdgeRuleMode... modes) {
			this.adjCodePresent = adjCodePresent;
			this.adjCodeEmpty = adjCodeEmpty;
			this.selector = selector;
			this.variants = variants;
			this.modes = Arrays.asList(modes);
		}

		public Function<FPTileTransitionVariantLayout, Optional<FPTileSpriteLayoutVariant>> getSelector() {
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

		public Function<FPTileTransitionVariantLayout, Optional<FPTileSpriteLayoutVariant>> getSelector() {
			return rule.selector;
		}

		public int getVariant() {
			return variant;
		}

		@Override
		public int hashCode() {
			return Objects.hash(rule, variant);
		}
	}

	public static EnumMap<TileEdgeRuleMode, List<List<TileEdgeRuleParam>>> tileRulesByMode = new EnumMap<>(
			TileEdgeRuleMode.class);

	static {
		for (TileEdgeRuleMode ruleMode : TileEdgeRuleMode.values()) {
			List<List<TileEdgeRuleParam>> tileRules;
			tileRulesByMode.put(ruleMode, tileRules = new ArrayList<>());
			IntStream.range(0, 256).forEach(i -> tileRules.add(new ArrayList<>()));
			for (TileEdgeRule rule : TileEdgeRule.values()) {
				if (!rule.modes.contains(ruleMode)) {
					continue;
				}
				int adjCodePresent = rule.adjCodePresent;
				int adjCodeEmpty = rule.adjCodeEmpty;
				for (int variant = 0; variant < rule.variants; variant++) {
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
					int nextAdjCodePresent = ((adjCodePresent << 2) | (adjCodePresent >> 6)) & 0xFF;
					int nextAdjCodeEmpty = ((adjCodeEmpty << 2) | (adjCodeEmpty >> 6)) & 0xFF;
					if (nextAdjCodePresent == adjCodePresent && nextAdjCodeEmpty == adjCodeEmpty) {
						break;
					}
					adjCodePresent = nextAdjCodePresent;
					adjCodeEmpty = nextAdjCodeEmpty;
				}
			}
		}
	}

	private static class TileCell {
		int row, col;
		int layer;
		BSTile tile;
		TileRendererFactory factory;
		boolean hidden;
	}

	private static class TileEdgeCell {
		int row, col;
		int layer;
		public MapPosition pos;
		TileRendererFactory factory;
		int adjCode = 0;
		List<TileEdgeRuleParam> params = null;

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (Direction dir : Direction.values()) {
				if ((adjCode & dir.adjCode()) != 0) {
					sb.append(dir.name()).append(" ");
				}
			}
			return sb.toString();
		}
	}

	public interface TileRenderProcess {
		void tileCenter(Random rand, Consumer<MapRenderable> register, TileCell cell);

		void tileEdge(Random rand, Consumer<MapRenderable> register, TileEdgeCell cell);

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
		public void tileEdge(Random rand, Consumer<MapRenderable> register, TileEdgeCell cell) {
			if (protoVariants.emptyTransitions) {
				return;
			}

			FPTileTransitions transitions = protoVariants.transition.get();

			rand.setSeed(getRandomSeed(cell.row, cell.col, cell.layer, cell.adjCode));

			// TODO figure out why some tiles do not have an overlay!
			if (transitions.layout.overlay.isPresent()) {
				FPTileTransitionVariantLayout overlay = transitions.layout.overlay.get();
				for (TileEdgeRuleParam param : cell.params) {
					Optional<FPTileSpriteLayoutVariant> optVariant = param.getSelector().apply(overlay);
					if (optVariant.isPresent()) {
						FPTileSpriteLayoutVariant variant = optVariant.get();

						int frame = rand.nextInt(variant.count);

						register.accept(
								new MapSprite(variant.defineImage(param.variant, frame), Layer.DECALS, cell.pos));
					}
				}
			}

			if (transitions.layout.background.isPresent()) {
				FPTileTransitionVariantLayout background = transitions.layout.background.get();
				for (TileEdgeRuleParam param : cell.params) {
					Optional<FPTileSpriteLayoutVariant> optVariant = param.getSelector().apply(background);
					if (optVariant.isPresent()) {
						FPTileSpriteLayoutVariant variant = optVariant.get();

						int frame = rand.nextInt(variant.count);

						register.accept(
								new MapSprite(variant.defineImage(param.variant, frame), Layer.UNDER_TILES, cell.pos));
					}
				}
			}
		}

		@Override
		public void initAtlas(Consumer<ImageDef> register) {
			protoVariantsMainSize1.ifPresent(fp -> fp.getDefs(register));
			if (!protoVariants.emptyTransitions) {
				protoVariants.transition.get().layout.overlay.ifPresent(fp -> fp.getDefs(register));
				protoVariants.transition.get().layout.background.ifPresent(fp -> fp.getDefs(register));
			}
		}
	}

	public class TileRenderProcessMaterial implements TileRenderProcess {
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
		public void tileEdge(Random rand, Consumer<MapRenderable> register, TileEdgeCell cell) {

			FPMaterialTextureParameters material = protoVariants.materialBackground.get();
			int tw = material.getTexWidthTiles();
			int th = material.getTexHeightTiles();

			rand.setSeed(getRandomSeed(cell.row / th, cell.col / tw, cell.layer, 0));
			int materialFrame = rand.nextInt(material.getLimitedCount());
			MaterialDef materialDef = material.defineMaterial(materialFrame);

			FPTileTransitions transitions = protoVariants.transition.get();
			Optional<FPTileTransitionVariantLayout> optOverlay = transitions.layout.overlay;
			FPTileTransitionVariantLayout mask = transitions.layout.mask.get();

			rand.setSeed(getRandomSeed(cell.row, cell.col, cell.layer, cell.adjCode));

			for (TileEdgeRuleParam param : cell.params) {
				Optional<FPTileSpriteLayoutVariant> optVariantMask = param.getSelector().apply(mask);
				Optional<FPTileSpriteLayoutVariant> optVariantOverlay = optOverlay
						.flatMap(o -> param.getSelector().apply(o));

				OptionalInt frame = OptionalInt.empty();

				if (optVariantMask.isPresent()) {
					FPTileSpriteLayoutVariant variantMask = optVariantMask.get();

					frame = OptionalInt.of(rand.nextInt(variantMask.count));

					register.accept(new MapMaterialMaskedTile(materialDef,
							variantMask.defineImage(param.variant, frame.getAsInt()), cell.row % th, cell.col % tw,
							cell.pos));

				}

				if (optVariantOverlay.isPresent()) {
					FPTileSpriteLayoutVariant variantOverlay = optVariantOverlay.get();

					if (frame.isEmpty()) {
						frame = OptionalInt.of(rand.nextInt(variantOverlay.count));
					}

					register.accept(new MapSprite(variantOverlay.defineImage(param.variant, frame.getAsInt()),
							Layer.DECALS, cell.pos));
				}
			}

			rand.setSeed(getRandomSeed(cell.row, cell.col, cell.layer, cell.adjCode));
		}

		@Override
		public void initAtlas(Consumer<ImageDef> register) {
			protoVariants.materialBackground.get().getDefs().forEach(register);
			protoVariants.transition.ifPresent(fp -> fp.layout.overlay.ifPresent(fp2 -> fp2.getDefs(register)));
			protoVariants.transition.ifPresent(fp -> fp.layout.mask.ifPresent(fp2 -> fp2.getDefs(register)));
		}
	}

	public static void createAllRenderers(Consumer<MapRenderable> register, List<MapTile> allTiles) {

		// TODO how do I decide which edge factory for matching layers? (example,
		// hazard-concrete-left/right)

		// XXX this is terrible
		// TODO make a predictable random method consistent for every coordinate
		Random rand = new Random();

		// <layer, <row, col, cell>>
		LinkedHashMap<Integer, Table<Integer, Integer, TileCell>> tileMaps = new LinkedHashMap<>();

		// XXX should I also do render order (left to right, top to bottom)?
		List<MapTile> tileOrder = allTiles.stream().filter(t -> !t.getFactory().isUnknown())
				.sorted(Comparator.comparing(t -> t.getFactory().protoLayer)).collect(Collectors.toList());

		// <layer, <row, col, cell>>
		LinkedHashMap<Integer, Table<Integer, Integer, TileEdgeCell>> tileEdgeMaps = new LinkedHashMap<>();

		tileOrder.stream().mapToInt(t -> t.getFactory().protoLayer).distinct().forEach(i -> {
			tileMaps.put(i, HashBasedTable.create());
			tileEdgeMaps.put(i, HashBasedTable.create());
		});
		tileOrder.stream().flatMap(t -> t.getFactory().protoTransitionMergesWithTile.stream()).mapToInt(t -> t.protoLayer)
				.distinct().forEach(i -> {
					tileMaps.put(i, HashBasedTable.create());
					tileEdgeMaps.put(i, HashBasedTable.create());
				});

		TreeSet<Integer> activeLayers = new TreeSet<>();
		Multimap<Integer, TileCell> cellLayers = ArrayListMultimap.create();
		Multimap<Integer, TileEdgeCell> edgeCellLayers = ArrayListMultimap.create();

		List<TileCell> cells = new ArrayList<>();

		// Populate tile map
		for (MapTile mapTile : tileOrder) {
			TileRendererFactory factory = mapTile.getFactory();
			if (factory.protoTransitionMergesWithTile.isPresent()) {
				TileRendererFactory mergeFactory = factory.protoTransitionMergesWithTile.get();

				TileCell cell = new TileCell();
				BSPosition pos = mapTile.fromBlueprint().position;
				cell.row = (int) pos.y;
				cell.col = (int) pos.x;
				cell.layer = mergeFactory.protoLayer;
				cell.tile = mapTile.fromBlueprint();
				cell.factory = mergeFactory;
				cell.hidden = true;
				tileMaps.get(cell.layer).put(cell.row, cell.col, cell);
				activeLayers.add(cell.layer);
				cellLayers.put(cell.layer, cell);
				cells.add(cell);
			}

			TileCell cell = new TileCell();
			BSPosition pos = mapTile.fromBlueprint().position;
			cell.row = (int) pos.y;
			cell.col = (int) pos.x;
			cell.layer = factory.protoLayer;
			cell.tile = mapTile.fromBlueprint();
			cell.factory = factory;
			cell.hidden = false;
			tileMaps.get(cell.layer).put(cell.row, cell.col, cell);
			activeLayers.add(cell.layer);
			cellLayers.put(cell.layer, cell);
			cells.add(cell);
		}

		cells.sort(Comparator.comparing(c -> c.layer));

		// Populate edge maps
		for (TileCell cell : cells) {
			BSPosition pos = cell.tile.position;
			int row = (int) pos.y;
			int col = (int) pos.x;
			Table<Integer, Integer, TileCell> tileMap = tileMaps.get(cell.layer);
			Table<Integer, Integer, TileEdgeCell> edgeMap = tileEdgeMaps.get(cell.layer);

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
					edgeCell.pos = MapPosition.byUnit(adjCol, adjRow);
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
				if (cell.hidden) {
					continue;
				}
				cell.factory.renderProcess.tileCenter(rand, register, cell);
			}

			// Render tile edges
			for (TileEdgeCell cell : edgeCellLayers.get(layer)) {
				TileEdgeRuleMode ruleMode = null;
				if (cell.factory.protoTransitionMergesWithTile.isPresent()) {
					TileRendererFactory mergeFactory = cell.factory.protoTransitionMergesWithTile.get();
					TileEdgeCell mergeCell = tileEdgeMaps.get(mergeFactory.protoLayer).get(cell.row, cell.col);
					if (mergeCell != null && mergeCell.params.stream().anyMatch(
							p -> p.rule == TileEdgeRule.U_TRANSITION || p.rule == TileEdgeRule.O_TRANSITION)) {
						ruleMode = TileEdgeRuleMode.MERGED_UO;
					}
				}
				if (ruleMode == null) {
					ruleMode = cell.factory.protoDoubleSided ? TileEdgeRuleMode.WITH_DOUBLE
							: TileEdgeRuleMode.NO_DOUBLE;
				}
				List<List<TileEdgeRuleParam>> tileRules = tileRulesByMode.get(ruleMode);
				List<TileEdgeRuleParam> params = tileRules.get(cell.adjCode);
				cell.params = params;

				cell.factory.renderProcess.tileEdge(rand, register, cell);
			}
		}

	}

	public static long getRandomSeed(int row, int col, int layer, int adjCode) {
		return ((row * 73856093) ^ (col * 19349663) ^ (layer * 83492791) ^ (adjCode * 123456789));
	}

	public void initAtlas(Consumer<ImageDef> register) {
		renderProcess.initAtlas(register);
	}

	protected String name;
	protected Profile profile;
	protected TilePrototype prototype;
	protected List<ManifestModInfo	> mods;

	private FPTileTransitionsVariants protoVariants;
	private Optional<FPTileMainPictures> protoVariantsMainSize1;
	private int protoLayer;
	private Optional<String> protoTransitionMergesWithTileID;
	private Optional<TileRendererFactory> protoTransitionMergesWithTile;
	private boolean protoDoubleSided;

	private TileRenderProcess renderProcess = null;

	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapTile tile) {
	}

	public Profile getProfile() {
		return profile;
	}

	public String getName() {
		return name;
	}

	public TilePrototype getPrototype() {
		return prototype;
	}

	public List<ManifestModInfo> getMods() {
		return mods;
	}

	public void initFromPrototype(DataTable table) {
		protoLayer = prototype.lua().get("layer").checkint();
		protoVariants = new FPTileTransitionsVariants(profile, prototype.lua().get("variants"), 10);
		protoVariantsMainSize1 = protoVariants.main.stream().filter(fp -> fp.size == 1).findFirst();
		protoTransitionMergesWithTileID = FPUtils.optString(prototype.lua().get("transition_merges_with_tile"));
		protoTransitionMergesWithTile = protoTransitionMergesWithTileID.map(k -> resolveMergeTileID(k));

		if (protoVariants.materialBackground.isPresent()) {
			renderProcess = new TileRenderProcessMaterial();
		} else if (!protoVariants.main.isEmpty()) {
			renderProcess = new TileRenderProcessMain();
		}

		protoDoubleSided = protoVariants.transition.stream()
				.flatMap(fp -> ImmutableList.of(fp.layout.background, fp.layout.mask, fp.layout.overlay).stream())
				.anyMatch(fp -> fp.flatMap(fp2 -> fp2.doubleSide).isPresent());
	}

	private TileRendererFactory resolveMergeTileID(String name) {
		if (profile.getFactorioManager() == null) {
			return new UnknownTileRendering(name);
		}

		List<Profile> profileOrder;
		if (profile.isVanilla()) {
			profileOrder = ImmutableList.of(profile);
		} else {
			profileOrder = ImmutableList.of(profile, profile.getFactorioManager().getProfileVanilla());
		}
		ModdingResolver resolver = ModdingResolver.byProfileOrder(profile.getFactorioManager(), profileOrder, false);
		TileRendererFactory mergeTile = resolver.resolveFactoryTileName(name);
		
		if (mergeTile.isUnknown()) {
			throw new IllegalStateException("Tile transition merge with tile not found: " + name);
		}
		
		return mergeTile;
	}

	public boolean isUnknown() {
		return false;
	}

	public void setProfile(Profile profile) {
		this.profile = profile;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setMods(List<ManifestModInfo> mods) {
		this.mods = mods;
	}

	public void setPrototype(TilePrototype prototype) {
		this.prototype = prototype;
	}
}
