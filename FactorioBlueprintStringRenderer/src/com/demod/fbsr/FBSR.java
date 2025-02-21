package com.demod.fbsr;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.Renderer;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.dcba.CommandReporting;
import com.demod.factorio.Config;
import com.demod.factorio.DataTable;
import com.demod.factorio.ItemToPlace;
import com.demod.factorio.ModInfo;
import com.demod.factorio.TotalRawCalculator;
import com.demod.factorio.Utils;
import com.demod.factorio.prototype.DataPrototype;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.factorio.prototype.ItemPrototype;
import com.demod.factorio.prototype.RecipePrototype;
import com.demod.factorio.prototype.TilePrototype;
import com.demod.fbsr.WirePoints.WirePoint;
import com.demod.fbsr.WorldMap.RailEdge;
import com.demod.fbsr.WorldMap.RailNode;
import com.demod.fbsr.bs.BSBlueprint;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.BSMetaEntity;
import com.demod.fbsr.bs.BSPosition;
import com.demod.fbsr.bs.BSTile;
import com.demod.fbsr.bs.BSWire;
import com.demod.fbsr.entity.ErrorRendering;
import com.demod.fbsr.gui.GUIStyle;
import com.demod.fbsr.map.MapDebugEntityPlacement;
import com.demod.fbsr.map.MapDebugTilePlacement;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapFoundationGrid;
import com.demod.fbsr.map.MapGrid;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRect;
import com.demod.fbsr.map.MapRect3D;
import com.demod.fbsr.map.MapRenderable;
import com.demod.fbsr.map.MapSprite;
import com.demod.fbsr.map.MapTile;
import com.demod.fbsr.map.MapWire;
import com.demod.fbsr.map.MapWireShadow;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import com.google.common.collect.Table;

public class FBSR {
	private static final Logger LOGGER = LoggerFactory.getLogger(FBSR.class);

	private static final long TARGET_FILE_SIZE = 10 << 20; // 10MB
	private static final float ESTIMATED_JPG_PIXELS_PER_BYTE = 3.5f; // Based on measuring large JPG renders
	private static final long MAX_WORLD_RENDER_PIXELS = (long) (TARGET_FILE_SIZE * ESTIMATED_JPG_PIXELS_PER_BYTE);

	public static final Color GROUND_COLOR = new Color(40, 40, 40);
	public static final Color GRID_COLOR = new Color(0xffe6c0);

	private static volatile String version = null;

	private static final Map<String, Color> itemColorCache = new HashMap<>();

	public static final double TILE_SIZE = 64.0;

	private static volatile boolean initialized = false;

	private static void addToItemAmount(Map<String, Double> items, String itemName, double add) {
		double amount = items.getOrDefault(itemName, 0.0);
		amount += add;
		items.put(itemName, amount);
	}

	public static Map<String, Double> generateTotalItems(BSBlueprint blueprint) {

		Map<String, Double> ret = new LinkedHashMap<>();
		for (BSEntity entity : blueprint.entities) {
			String entityName = entity.name;
			EntityRendererFactory entityFactory = FactorioManager.lookupEntityFactoryForName(entityName);
			if (entityFactory.isUnknown()) {
				continue;
			}

			EntityPrototype entityPrototype = entityFactory.getPrototype();

			Optional<ItemToPlace> primaryItem = entityPrototype.getPrimaryItem();
			if (primaryItem.isEmpty()) {
				LOGGER.warn("MISSING ENTITY ITEM: {}", entityName);
				continue;
			}

			addToItemAmount(ret, primaryItem.get().getItem(), primaryItem.get().getCount());

			Multiset<String> modules = MapEntity.findModules(entity);
			for (Multiset.Entry<String> entry : modules.entrySet()) {
				addToItemAmount(ret, entry.getElement(), entry.getCount());
			}
		}
		for (BSTile tile : blueprint.tiles) {
			String tileName = tile.name;
			TileRendererFactory tileFactory = FactorioManager.lookupTileFactoryForName(tileName);
			TilePrototype tilePrototype = tileFactory.getPrototype();

			Optional<ItemToPlace> primaryItem = tilePrototype.getPrimaryItem();
			if (primaryItem.isEmpty()) {
				LOGGER.warn("MISSING TILE ITEM: {}", tilePrototype.getName());
				continue;
			}

			addToItemAmount(ret, primaryItem.get().getItem(), primaryItem.get().getCount());
		}
		return ret;
	}

	public static Map<String, Double> generateTotalRawItems(Map<String, Double> totalItems) {
		DataTable baseTable = FactorioManager.getBaseData().getTable();
		Map<String, RecipePrototype> recipes = baseTable.getRecipes();
		Map<String, Double> ret = new LinkedHashMap<>();
		TotalRawCalculator calculator = new TotalRawCalculator(recipes);
		for (Entry<String, Double> entry : totalItems.entrySet()) {
			String recipeName = entry.getKey();
			double recipeAmount = entry.getValue();
			baseTable.getRecipe(recipeName).ifPresent(r -> {
				double multiplier = recipeAmount / r.getOutputs().get(recipeName);
				Map<String, Double> totalRaw = calculator.compute(r);
				for (Entry<String, Double> entry2 : totalRaw.entrySet()) {
					String itemName = entry2.getKey();
					double itemAmount = entry2.getValue();
					addToItemAmount(ret, itemName, itemAmount * multiplier);
				}
			});
		}
		return ret;
	}

	private static Color getItemLogisticColor(String itemName) {

		return itemColorCache.computeIfAbsent(itemName, k -> {
			Optional<ItemPrototype> optProto = FactorioManager.lookupItemByName(k);
			if (!optProto.isPresent()) {
				LOGGER.warn("ITEM MISSING FOR LOGISTICS: {}", k);
				return Color.MAGENTA;
			}
			DataPrototype prototype = optProto.get();
			BufferedImage image = prototype.getTable().getData().getWikiIcon(prototype);
			Color color = RenderUtils.getAverageColor(image);
			// return new Color(color.getRGB() | 0xA0A0A0);
			// return color.brighter().brighter();
			float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
			return Color.getHSBColor(hsb[0], Math.max(0.25f, hsb[1]), Math.max(0.5f, hsb[2]));
			// return Color.getHSBColor(hsb[0], Math.max(1f, hsb[1]),
			// Math.max(0.75f, hsb[2]));
		});
	}

	public static String getVersion() {
		if (version == null) {
			ModInfo baseInfo;
			try (FileInputStream fis = new FileInputStream(new File(
					Config.get().getJSONObject("factorio_manager").getString("install"), "data/base/info.json"))) {
				baseInfo = new ModInfo(Utils.readJsonFromStream(fis));
				version = baseInfo.getVersion();
			} catch (JSONException | IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
		return version;
	}

	public static synchronized void initialize() throws IOException {
		if (initialized) {
			return;
		}
		initialized = true;
		FactorioManager.initializePrototypes();
		FactorioManager.initializeFactories();
	}

	private static void populateRailBlocking(WorldMap map) {
		map.getRailNodes().cellSet().stream().filter(c -> c.getValue().hasSignals()).forEach(c -> {
			RailNode blockingNode = c.getValue();
			Set<Direction> signals = blockingNode.getSignals();
			for (Direction signalDir : signals) {
				Direction blockingDir = signalDir.back();
				if (signals.contains(blockingDir)) {
					continue;
				}

				{
					Queue<RailEdge> work = new ArrayDeque<>();
					work.addAll(blockingNode.getOutgoingEdges(blockingDir));
					while (!work.isEmpty()) {
						RailEdge edge = work.poll();
						if (edge.isBlocked()) {
							continue;
						}
						edge.setBlocked(true);
						RailNode node = map.getRailNode(edge.getEndPos()).get();
						if (node.hasSignals()) {
							continue;
						}
						if (node.getIncomingEdges(edge.getEndDir()).stream().allMatch(e -> e.isBlocked())) {
							work.addAll(node.getOutgoingEdges(edge.getEndDir().back()));
						}
					}
				}

				{
					Queue<RailEdge> work = new ArrayDeque<>();
					work.addAll(blockingNode.getIncomingEdges(blockingDir.back()));
					while (!work.isEmpty()) {
						RailEdge edge = work.poll();
						if (edge.isBlocked()) {
							continue;
						}
						edge.setBlocked(true);
						RailNode node = map.getRailNode(edge.getStartPos()).get();
						if (node.hasSignals()) {
							continue;
						}
						if (node.getOutgoingEdges(edge.getStartDir()).stream().allMatch(e -> e.isBlocked())) {
							work.addAll(node.getIncomingEdges(edge.getStartDir().back()));
						}
					}
				}

				// for (RailEdge startEdge :
				// blockingNode.getOutgoingEdges(blockingDir)) {
				// startEdge.setBlocked(true);
				// RailNode node = map.getRailNode(startEdge.getEndPos()).get();
				// Direction dir = startEdge.getEndDir();
				// Collection<RailEdge> edges;
				// while (!node.hasSignals() && ((edges =
				// node.getOutgoingEdges(dir)).size() == 1)) {
				// RailEdge edge = edges.iterator().next();
				// if (edge.isBlocked()) {
				// break;
				// }
				// edge.setBlocked(true);
				// node = map.getRailNode(edge.getEndPos()).get();
				// dir = edge.getEndDir();
				// }
				// }
				//
				// for (RailEdge startEdge :
				// blockingNode.getIncomingEdges(blockingDir)) {
				// startEdge.setBlocked(true);
				// RailNode node = map.getRailNode(startEdge.getEndPos()).get();
				// Direction dir = startEdge.getEndDir();
				// Collection<RailEdge> edges;
				// while (!node.hasSignals() && ((edges =
				// node.getIncomingEdges(dir)).size() == 1)) {
				// RailEdge edge = edges.iterator().next();
				// if (edge.isBlocked()) {
				// break;
				// }
				// edge.setBlocked(true);
				// node = map.getRailNode(edge.getEndPos()).get();
				// dir = edge.getEndDir();
				// }
				// }
			}
		});
	}

	private static void populateRailStationLogistics(WorldMap map) {
		map.getRailNodes().cellSet().stream().filter(c -> c.getValue().getStation().isPresent()).forEach(c -> {
			RailNode stationNode = c.getValue();
			Direction stationDir = stationNode.getStation().get();

			{
				Queue<RailEdge> work = new ArrayDeque<>();
				work.addAll(stationNode.getOutgoingEdges(stationDir));
				work.addAll(stationNode.getOutgoingEdges(stationDir.back()));
				while (!work.isEmpty()) {
					RailEdge edge = work.poll();
					if (edge.isBlocked() || edge.isOutput()) {
						continue;
					}
					edge.setOutput(true);
					RailNode node = map.getRailNode(edge.getEndPos()).get();
					if (node.getIncomingEdges(edge.getEndDir()).stream().allMatch(e -> e.isOutput())) {
						work.addAll(node.getOutgoingEdges(edge.getEndDir().back()));
					}
				}
			}

			{
				Queue<RailEdge> work = new ArrayDeque<>();
				work.addAll(stationNode.getIncomingEdges(stationDir.back()));
				while (!work.isEmpty()) {
					RailEdge edge = work.poll();
					if (edge.isBlocked() || edge.isInput()) {
						continue;
					}
					edge.setInput(true);
					RailNode node = map.getRailNode(edge.getStartPos()).get();
					if (node.getOutgoingEdges(edge.getStartDir()).stream().allMatch(e -> e.isInput())) {
						work.addAll(node.getIncomingEdges(edge.getStartDir().back()));
					}
				}
			}
		});
	}

	private static void populateReverseLogistics(WorldMap map) {
		Table<Integer, Integer, LogisticGridCell> logisticGrid = map.getLogisticGrid();
		logisticGrid.cellSet().forEach(c -> {
			// TODO fixed-point math
			MapPosition pos = MapPosition.byUnit(c.getRowKey() / 2.0 + 0.25, c.getColumnKey() / 2.0 + 0.25);
			LogisticGridCell cell = c.getValue();
			cell.getMove().ifPresent(d -> {
				map.getLogisticGridCell(d.offset(pos, 0.5)).filter(mc -> mc.acceptMoveFrom(d))
						.ifPresent(mc -> mc.addMovedFrom(d.back()));
			});
			cell.getWarps().ifPresent(l -> {
				for (MapPosition p : l) {
					map.getLogisticGridCell(p).ifPresent(mc -> mc.addWarpedFrom(pos));
				}
			});
		});
	}

	private static void populateTransitLogistics(WorldMap map, boolean populateInputs, boolean populateOutputs) {
		Table<Integer, Integer, LogisticGridCell> logisticGrid = map.getLogisticGrid();
		ArrayDeque<Entry<MapPosition, LogisticGridCell>> work = new ArrayDeque<>();

		if (populateOutputs) {
			logisticGrid.cellSet().stream().filter(c -> c.getValue().isTransitStart()).forEach(c -> {
				Set<String> outputs = c.getValue().getOutputs().get();
				for (String item : outputs) {
					work.add(new SimpleEntry<>(map.getLogisticCellPosition(c), c.getValue()));
					while (!work.isEmpty()) {
						Entry<MapPosition, LogisticGridCell> pair = work.pop();
						MapPosition cellPos = pair.getKey();
						LogisticGridCell cell = pair.getValue();
						if (cell.addTransit(item) && !cell.isBannedOutput(item)) {
							cell.getMove().ifPresent(d -> {
								MapPosition nextCellPos = d.offset(cellPos, 0.5);
								map.getLogisticGridCell(nextCellPos)
										.filter(nc -> !nc.isBlockTransit() && nc.acceptMoveFrom(d))
										.ifPresent(next -> work.add(new SimpleEntry<>(nextCellPos, next)));
							});
							cell.getWarps().ifPresent(l -> {
								for (MapPosition p : l) {
									map.getLogisticGridCell(p)
											.filter(nc -> !nc.isBlockTransit()
													&& !(nc.getMove().isPresent() && cell.isBlockWarpFromIfMove())
													&& !(cell.getMove().isPresent() && nc.isBlockWarpToIfMove()))
											.ifPresent(next -> work.add(new SimpleEntry<>(p, next)));
								}
							});
						}
					}
				}
			});
		}

		if (populateInputs) {
			logisticGrid.cellSet().stream().filter(c -> c.getValue().isTransitEnd()).forEach(c -> {
				Set<String> inputs = c.getValue().getInputs().get();
				for (String item : inputs) {
					work.add(new SimpleEntry<>(map.getLogisticCellPosition(c), c.getValue()));
					while (!work.isEmpty()) {
						Entry<MapPosition, LogisticGridCell> pair = work.pop();
						MapPosition cellPos = pair.getKey();
						LogisticGridCell cell = pair.getValue();
						if (cell.addTransit(item)) {
							cell.getMovedFrom().ifPresent(l -> {
								for (Direction d : l) {
									MapPosition nextCellPos = d.offset(cellPos, 0.5);
									map.getLogisticGridCell(nextCellPos).filter(nc -> !nc.isBlockTransit())
											.ifPresent(next -> work.add(new SimpleEntry<>(nextCellPos, next)));
								}
							});
							cell.getWarpedFrom().ifPresent(l -> {
								for (MapPosition p : l) {
									map.getLogisticGridCell(p).filter(nc -> !nc.isBlockTransit())
											.ifPresent(next -> work.add(new SimpleEntry<>(p, next)));
								}
							});
						}
					}
				}
			});
		}

	}

	// FIXME the generic type checking is all screwed up
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static RenderResult renderBlueprint(RenderRequest request) {

		BSBlueprint blueprint = request.getBlueprint();
		CommandReporting reporting = request.getReporting();

		LOGGER.info("Rendering {} {}", blueprint.label.orElse("Untitled Blueprint"), blueprint.version);
		long startMillis = System.currentTimeMillis();

		WorldMap map = new WorldMap();

		map.setAltMode(request.show.altMode);

		List<MapEntity> mapEntities = new ArrayList<MapEntity>();
		List<MapTile> mapTiles = new ArrayList<MapTile>();
		Map<Integer, MapEntity> mapEntityByNumber = new HashMap<>();

		for (BSMetaEntity metaEntity : blueprint.entities) {
			EntityRendererFactory factory = FactorioManager.lookupEntityFactoryForName(metaEntity.name);
			BSEntity entity;
			try {
				if (metaEntity.isLegacy()) {
					entity = factory.parseEntityLegacy(metaEntity.getLegacy());
				} else {
					entity = factory.parseEntity(metaEntity.getJson());
				}
			} catch (Exception e) {
				metaEntity.setParseException(Optional.of(e));
				entity = metaEntity;
			}
			if (metaEntity.getParseException().isPresent()) {
				factory = new ErrorRendering();
				reporting.addException(metaEntity.getParseException().get(), entity.name + " " + entity.entityNumber);
			}
			MapEntity mapEntity = new MapEntity(entity, factory);
			mapEntities.add(mapEntity);
			mapEntityByNumber.put(entity.entityNumber, mapEntity);
		}
		for (BSTile tile : blueprint.tiles) {
			TileRendererFactory factory = FactorioManager.lookupTileFactoryForName(tile.name);
			MapTile mapTile = new MapTile(tile, factory);
			mapTiles.add(mapTile);
		}

		map.setFoundation(mapTiles.stream().anyMatch(t -> t.getFactory().getPrototype().isFoundation()));

		mapEntities.forEach(t -> {
			try {
				t.getFactory().populateWorldMap(map, t);
			} catch (Exception e) {
				reporting.addException(e, t.getFactory().getClass().getSimpleName() + ", " + t.fromBlueprint().name);
			}
		});
		mapTiles.forEach(t -> {
			try {
				t.getFactory().populateWorldMap(map, t);
			} catch (Exception e) {
				reporting.addException(e, t.getFactory().getClass().getSimpleName() + ", " + t.fromBlueprint().name);
			}
		});

		mapEntities.forEach(t -> {
			try {
				t.getFactory().populateLogistics(map, t);
			} catch (Exception e) {
				reporting.addException(e, t.getFactory().getClass().getSimpleName() + ", " + t.fromBlueprint().name);
			}
		});

		populateReverseLogistics(map);
		populateTransitLogistics(map, request.show.pathInputs, request.show.pathOutputs);

		populateRailBlocking(map);
		populateRailStationLogistics(map);

		List<MapRenderable> renderers = new ArrayList<>();
		Consumer<MapRenderable> register = r -> renderers.add(r);

		TileRendererFactory.createAllRenderers(renderers::add, mapTiles);

		mapTiles.forEach(t -> {
			try {
				t.getFactory().createRenderers(register, map, t);
			} catch (Exception e) {
				reporting.addException(e, t.getFactory().getClass().getSimpleName() + ", " + t.fromBlueprint().name);
			}
		});

		mapEntities.forEach(t -> {
			try {
				t.getFactory().createRenderers(register, map, t);
			} catch (Exception e) {
				reporting.addException(e, t.getFactory().getClass().getSimpleName() + ", " + t.fromBlueprint().name);
			}
		});

		if (map.isAltMode()) {
			mapEntities.forEach(t -> {
				try {
					t.getFactory().createModuleIcons(register, map, t);
				} catch (Exception e) {
					reporting.addException(e,
							t.getFactory().getClass().getSimpleName() + ", " + t.fromBlueprint().name);
				}
			});
		}

		Map<Integer, Double> connectorOrientations = new HashMap<>();
		int[] wireEntityNumbers = blueprint.wires.stream()
				.flatMapToInt(w -> IntStream.of(w.firstEntityNumber, w.secondEntityNumber)).distinct().toArray();
		for (int entityNumber : wireEntityNumbers) {
			MapEntity mapEntity = mapEntityByNumber.get(entityNumber);
			List<MapEntity> wired = blueprint.wires.stream().flatMapToInt(w -> {
				if (w.firstEntityNumber == entityNumber) {
					return IntStream.of(w.secondEntityNumber);
				} else if (w.secondEntityNumber == entityNumber) {
					return IntStream.of(w.firstEntityNumber);
				} else {
					return IntStream.of();
				}
			}).mapToObj(mapEntityByNumber::get).collect(Collectors.toList());

			double orientation = mapEntity.getFactory().initWireConnector(register, mapEntity, wired);
			connectorOrientations.put(entityNumber, orientation);
		}

		for (BSWire wire : blueprint.wires) {
			try {
				MapEntity first = mapEntityByNumber.get(wire.firstEntityNumber);
				MapEntity second = mapEntityByNumber.get(wire.secondEntityNumber);

				double orientation1 = connectorOrientations.get(wire.firstEntityNumber);
				double orientation2 = connectorOrientations.get(wire.secondEntityNumber);

				Optional<WirePoint> firstPoint = first.getFactory().createWirePoint(register,
						first.fromBlueprint().position.createPoint(), orientation1, wire.firstWireConnectorId);
				Optional<WirePoint> secondPoint = second.getFactory().createWirePoint(register,
						second.fromBlueprint().position.createPoint(), orientation2, wire.secondWireConnectorId);

				if (!firstPoint.isPresent() || !secondPoint.isPresent()) {
					continue;// Probably something modded
				}

				renderers.add(new MapWire(firstPoint.get().getPosition(), secondPoint.get().getPosition(),
						firstPoint.get().getColor().getColor()));
				renderers.add(new MapWireShadow(firstPoint.get().getShadow(), secondPoint.get().getShadow()));
			} catch (Exception e) {
				reporting.addException(e, "Wire " + wire.firstEntityNumber + ", " + wire.firstWireConnectorId + ", "
						+ wire.secondEntityNumber + ", " + wire.secondWireConnectorId);
			}
		}

		showLogisticGrid(register, map, request.debug.pathItems);
		showRailLogistics(register, map, request.debug.pathRails);

		if (request.debug.entityPlacement) {
			mapEntities.forEach(e -> {
				renderers.add(new MapDebugEntityPlacement(e));
			});
			mapTiles.forEach(t -> {
				renderers.add(new MapDebugTilePlacement(t));
			});
		}

		boolean showGrid = !request.getGridLines().isEmpty();
		boolean gridFoundationMode = map.isFoundation() && !request.show.gridNumbers;
		boolean gridShowNumbers = !gridFoundationMode && request.show.gridNumbers;
		boolean gridAboveBelts = request.show.gridAboveBelts;

		double gridPadding = (showGrid && gridShowNumbers) ? 1 : 0;
		double worldPadding = 0.1;

		MapRect3D gridBounds = calculateGridBounds(mapEntities, mapTiles);

		Rectangle2D.Double screenBounds = new Rectangle2D.Double();
		screenBounds.setFrameFromDiagonal(gridBounds.getX1() - worldPadding - gridPadding,
				gridBounds.getY1() - gridBounds.getHeight() - worldPadding - gridPadding,
				gridBounds.getX2() + worldPadding + gridPadding, gridBounds.getY2() + worldPadding + gridPadding);

		if (request.isDontClipSprites()) {
			MapRect spriteBounds = MapRect.combineAll(renderers.stream().filter(r -> r instanceof MapSprite)
					.map(r -> ((MapSprite) r).getBounds()).collect(Collectors.toList()));

			double x1 = spriteBounds.getX();
			double y1 = spriteBounds.getY();
			double x2 = x1 + spriteBounds.getWidth();
			double y2 = y1 + spriteBounds.getHeight();

			screenBounds.setFrameFromDiagonal(Math.min(screenBounds.getMinX(), x1),
					Math.min(screenBounds.getMinY(), y1), Math.max(screenBounds.getMaxX(), x2),
					Math.max(screenBounds.getMaxY(), y2));
		}

		double worldRenderScale = 1;

		// Max scale limit
		if (request.getMaxScale().isPresent()) {
			worldRenderScale = request.getMaxScale().getAsDouble();
		}

		// Shrink down the scale to fit the max requirements
		int maxWidthPixels = request.getMaxWidth().orElse(Integer.MAX_VALUE);
		int maxHeightPixels = request.getMaxHeight().orElse(Integer.MAX_VALUE);
		long maxPixels = Math.min(MAX_WORLD_RENDER_PIXELS, (long) maxWidthPixels * (long) maxHeightPixels);

		if ((screenBounds.getWidth() * worldRenderScale * TILE_SIZE) > maxWidthPixels) {
			worldRenderScale *= (maxWidthPixels / (screenBounds.getWidth() * worldRenderScale * TILE_SIZE));
		}
		if ((screenBounds.getHeight() * worldRenderScale * TILE_SIZE) > maxHeightPixels) {
			worldRenderScale *= (maxHeightPixels / (screenBounds.getHeight() * worldRenderScale * TILE_SIZE));
		}
		if ((screenBounds.getWidth() * worldRenderScale * TILE_SIZE)
				* (screenBounds.getHeight() * worldRenderScale * TILE_SIZE) > maxPixels) {
			worldRenderScale *= Math.sqrt(maxPixels / ((screenBounds.getWidth() * worldRenderScale * TILE_SIZE)
					* (screenBounds.getHeight() * worldRenderScale * TILE_SIZE)));
		}

		// Expand the world to fit the min requirements
		int minWidthPixels = request.getMinWidth().orElse(0);
		int minHeightPixels = request.getMinHeight().orElse(0);

		if ((screenBounds.getWidth() * worldRenderScale * TILE_SIZE) < minWidthPixels) {
			double padding = (minWidthPixels - (screenBounds.getWidth() * worldRenderScale * TILE_SIZE))
					/ (worldRenderScale * TILE_SIZE);
			screenBounds.x -= padding / 2.0;
			screenBounds.width += padding;
		}
		if ((screenBounds.getHeight() * worldRenderScale * TILE_SIZE) < minHeightPixels) {
			double padding = (minHeightPixels - (screenBounds.getHeight() * worldRenderScale * TILE_SIZE))
					/ (worldRenderScale * TILE_SIZE);
			screenBounds.y -= padding / 2.0;
			screenBounds.height += padding;
		}

		int imageWidth = Math.max(minWidthPixels,
				Math.min(maxWidthPixels, (int) Math.round(screenBounds.getWidth() * worldRenderScale * TILE_SIZE)));
		int imageHeight = Math.max(minHeightPixels,
				Math.min(maxHeightPixels, (int) Math.round(screenBounds.getHeight() * worldRenderScale * TILE_SIZE)));
		LOGGER.info("\t{}x{} ({})", imageWidth, imageHeight, worldRenderScale);

		BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();

		BufferedImage shadowImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D shadowG = shadowImage.createGraphics();
		AffineTransform noXform = g.getTransform();

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

		g.scale(image.getWidth() / screenBounds.getWidth(), image.getHeight() / screenBounds.getHeight());
		g.translate(-screenBounds.getX(), -screenBounds.getY());
		AffineTransform worldXform = g.getTransform();

		shadowG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		shadowG.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		shadowG.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		shadowG.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		shadowG.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		shadowG.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		shadowG.setTransform(worldXform);

		// Background
		if (request.getBackground().isPresent()) {
			g.setColor(request.getBackground().get());
			g.fill(screenBounds);
		}

		boolean gridTooSmall = (1 / worldRenderScale) > 5;
		if (gridTooSmall) {
			showGrid = false;
		}

		Layer gridLayer;
		if (request.show.gridAboveBelts) {
			gridLayer = Layer.GRID_ABOVE_BELTS;
		} else {
			gridLayer = Layer.GRID;
		}

		if (showGrid) {
			if (gridFoundationMode) {
				renderers.add(new MapFoundationGrid(mapTiles, request.getGridLines().get(), gridAboveBelts));
			} else {
				renderers.add(new MapGrid(gridBounds, request.getGridLines().get(), gridAboveBelts, gridShowNumbers));
			}
		}

		renderers.stream().filter(r -> r.getLayer() == Layer.SHADOW_BUFFER).forEach(r -> {
			try {
				r.render(shadowG);
			} catch (Exception e) {
				reporting.addException(e);
			}
		});
		shadowG.dispose();
		RenderUtils.halveAlpha(shadowImage);

		renderers.add(new Renderer(Layer.SHADOW_BUFFER, new MapRect3D(screenBounds, 0), true) {
			@Override
			public void render(Graphics2D g) throws Exception {
				AffineTransform tempXform = g.getTransform();
				g.setTransform(noXform);
				g.drawImage(shadowImage, 0, 0, null);

				g.setTransform(tempXform);
			}
		});

		renderers.stream().sorted((r1, r2) -> {
			int ret;

			ret = r1.getLayer().compareTo(r2.getLayer());
			if (ret != 0) {
				return ret;
			}

			MapRect3D b1 = r1.getBounds();
			MapRect3D b2 = r2.getBounds();

			ret = Double.compare(b1.y1, b2.y1);
			if (ret != 0) {
				return ret;
			}

			ret = Double.compare(b1.x1, b2.x1);
			if (ret != 0) {
				return ret;
			}

			ret = r1.getLayer().compareTo(r2.getLayer());
			return ret;
		}).forEach(r -> {
			try {
				r.render(g);
			} catch (Exception e) {
				reporting.addException(e);
			}
		});
		g.setTransform(worldXform);

		g.dispose();

		long endMillis = System.currentTimeMillis();
		LOGGER.info("\tRender Time {} ms", endMillis - startMillis);

		RenderResult result = new RenderResult(image, endMillis - startMillis, worldRenderScale);
		return result;
	}

	private static MapRect3D calculateGridBounds(List<MapEntity> mapEntities, List<MapTile> mapTiles) {

		int x1fp = 0, x2fp = 0, y1fp = 0, y2fp = 0, heightfp = 0;
		boolean first = true;

		if (!mapEntities.isEmpty()) {
			MapRect3D combined = MapRect3D
					.combineAll(mapEntities.stream().map(e -> e.getBounds()).collect(Collectors.toList()));

			x1fp = combined.getX1FP();
			y1fp = combined.getY1FP();
			x2fp = combined.getX2FP();
			y2fp = combined.getY2FP();
			heightfp = combined.getHeightFP();
			first = false;
		}

		if (!mapTiles.isEmpty()) {
			MapRect bounds = MapPosition
					.enclosingBounds(mapTiles.stream().map(t -> t.getPosition()).collect(Collectors.toList()));

			int xfp = bounds.getXFP();
			int yfp = bounds.getYFP();
			int wfp = bounds.getWidthFP();
			int hfp = bounds.getHeightFP();
			if (first) {
				x1fp = xfp;
				y1fp = yfp;
				x2fp = xfp + wfp;
				y2fp = yfp + hfp;
				first = false;

			} else {
				x1fp = Math.min(x1fp, xfp);
				y1fp = Math.min(y1fp, yfp);
				x2fp = Math.max(x2fp, xfp + wfp);
				y2fp = Math.max(y2fp, yfp + hfp);
			}
		}

		return MapRect3D.byFixedPoint(x1fp, x2fp, y1fp, y2fp, heightfp);
	}

	private static void showLogisticGrid(Consumer<MapRenderable> register, WorldMap map, boolean debug) {
		Table<Integer, Integer, LogisticGridCell> logisticGrid = map.getLogisticGrid();
		logisticGrid.cellSet().forEach(c -> {
			Point2D.Double pos = new Point2D.Double(c.getRowKey() / 2.0 + 0.25, c.getColumnKey() / 2.0 + 0.25);
			LogisticGridCell cell = c.getValue();
			cell.getTransits().ifPresent(s -> {
				if (s.isEmpty()) {
					return;
				}
				int i = 0;
				float width = 0.3f / s.size();
				for (String itemName : s) {
					double shift = ((i + 1) / (double) (s.size() + 1) - 0.5) / 3.0; // -0.25..0.25
					cell.getMove().filter(d -> map.getLogisticGridCell(d.offset(pos, 0.5))
							.map(LogisticGridCell::isAccepting).orElse(false)).ifPresent(d -> {
								register.accept(new Renderer(Layer.LOGISTICS_MOVE, pos, true) {
									@Override
									public void render(Graphics2D g) {
										Stroke ps = g.getStroke();
										g.setStroke(
												new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
										g.setColor(RenderUtils.withAlpha(getItemLogisticColor(itemName),
												255 - 127 / s.size()));
										g.draw(new Line2D.Double(d.right().offset(pos, shift),
												d.right().offset(d.offset(pos, 0.5), shift)));
										g.setStroke(ps);
									}
								});
							});
					i++;
				}

			});

			if (debug) {
				cell.getMovedFrom().ifPresent(l -> {
					for (Direction d : l) {
						Point2D.Double p = d.offset(pos, 0.5);
						register.accept(new Renderer(Layer.DEBUG_LA1, p, true) {

							@Override
							public void render(Graphics2D g) {
								Stroke ps = g.getStroke();
								g.setStroke(new BasicStroke(2 / (float) TILE_SIZE, BasicStroke.CAP_ROUND,
										BasicStroke.JOIN_ROUND));
								g.setColor(Color.cyan);
								g.draw(new Line2D.Double(pos, p));
								g.setStroke(ps);
							}

						});
					}
				});
				// TODO need shadows
//				cell.getWarpedFrom().ifPresent(l -> {
//					for (Point2D.Double p : l) {
//						if (cell.isBlockWarpToIfMove())
//							register.accept(RenderUtils.createWireRenderer(p, pos, Color.RED));
//						else if (map.getOrCreateLogisticGridCell(p).isBlockWarpFromIfMove())
//							register.accept(RenderUtils.createWireRenderer(p, pos, Color.MAGENTA));
//						else
//							register.accept(RenderUtils.createWireRenderer(p, pos, Color.GREEN));
//					}
//				});
			}
		});
	}

	private static void showRailLogistics(Consumer<MapRenderable> register, WorldMap map, boolean debug) {
		for (Entry<RailEdge, RailEdge> pair : map.getRailEdges()) {
			boolean input = pair.getKey().isInput() || pair.getValue().isInput();
			boolean output = pair.getKey().isOutput() || pair.getValue().isOutput();

			if (input || output) {
				RailEdge edge = pair.getKey();
				Point2D.Double p1 = edge.getStartPos();
				Direction d1 = edge.getStartDir();
				Point2D.Double p2 = edge.getEndPos();
				Direction d2 = edge.getEndDir();

				register.accept(new Renderer(Layer.LOGISTICS_RAIL_IO, edge.getStartPos(), true) {
					@Override
					public void render(Graphics2D g) {
						Shape path;
						if (edge.isCurved()) {
							double control = 1.7;
							Point2D.Double cc1 = d1.offset(p1, control);
							Point2D.Double cc2 = d2.offset(p2, control);
							path = new CubicCurve2D.Double(p1.x, p1.y, cc1.x, cc1.y, cc2.x, cc2.y, p2.x, p2.y);
						} else {
							path = new Line2D.Double(p1, p2);
						}

						Color color = (input && output) ? Color.yellow : input ? Color.green : Color.red;

						Stroke ps = g.getStroke();
						g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
						g.setColor(RenderUtils.withAlpha(color, 32));
						g.draw(path);
						g.setStroke(ps);
					}
				});
			}

			if (debug) {
				for (RailEdge edge : ImmutableList.of(pair.getKey(), pair.getValue())) {
					if (edge.isBlocked()) {
						continue;
					}

					Point2D.Double p1 = edge.getStartPos();
					Direction d1 = edge.getStartDir();
					Point2D.Double p2 = edge.getEndPos();
					Direction d2 = edge.getEndDir();

					register.accept(new Renderer(Layer.LOGISTICS_RAIL_IO, edge.getStartPos(), true) {

						@Override
						public void render(Graphics2D g) {
							Stroke ps = g.getStroke();
							g.setStroke(new BasicStroke(2 / (float) TILE_SIZE, BasicStroke.CAP_BUTT,
									BasicStroke.JOIN_ROUND));
							g.setColor(RenderUtils.withAlpha(Color.green, 92));
							g.draw(new Line2D.Double(d1.right().offset(p1), d2.left().offset(p2)));
							g.setStroke(ps);
						}
					});
				}
			}
		}

		if (debug) {
			map.getRailNodes().cellSet().forEach(c -> {
				Point2D.Double pos = new Point2D.Double(c.getRowKey() / 2.0, c.getColumnKey() / 2.0);
				RailNode node = c.getValue();

				register.accept(new Renderer(Layer.DEBUG_RAIL1, pos, true) {
					@Override
					public void render(Graphics2D g) {
						Stroke ps = g.getStroke();
						g.setStroke(
								new BasicStroke(1 / (float) TILE_SIZE, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));

						g.setColor(Color.cyan);
						g.setFont(new Font("Courier New", Font.PLAIN, 1));
						for (Direction dir : Direction.values()) {
							Collection<RailEdge> edges = node.getIncomingEdges(dir);
							if (!edges.isEmpty()) {
								Point2D.Double p1 = dir.right().offset(pos, 0.25);
								Point2D.Double p2 = dir.offset(p1, 0.5);
								g.draw(new Line2D.Double(p1, p2));
								g.drawString("" + edges.size(), (float) p2.x - 0.1f, (float) p2.y - 0.2f);
							}
						}

						g.setStroke(ps);
					}
				});

				register.accept(new Renderer(Layer.DEBUG_RAIL2, pos, true) {
					@Override
					public void render(Graphics2D g) {
						Stroke ps = g.getStroke();
						g.setStroke(
								new BasicStroke(1 / (float) TILE_SIZE, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));

						g.setColor(Color.magenta);
						g.setFont(new Font("Courier New", Font.PLAIN, 1));
						for (Direction dir : Direction.values()) {
							Collection<RailEdge> edges = node.getOutgoingEdges(dir);
							if (!edges.isEmpty()) {
								Point2D.Double p1 = dir.left().offset(pos, 0.25);
								Point2D.Double p2 = dir.offset(p1, 0.5);
								g.draw(new Line2D.Double(p1, p2));
								g.drawString("" + edges.size(), (float) p2.x - 0.1f, (float) p2.y - 0.2f);
							}
						}

						g.setStroke(ps);
					}
				});

			});
		}
	}
}
