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
import com.demod.fbsr.map.MapRect3D;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import com.google.common.collect.Table;

public class FBSR {

	public static class EntityRenderingTuple<E extends BSEntity> {
		public final E entity;
		public final EntityRendererFactory<E> factory;

		public EntityRenderingTuple(E entity, EntityRendererFactory<E> factory) {
			this.entity = entity;
			this.factory = factory;
		}

	}

	public static class TileRenderingTuple {
		public final BSTile tile;
		public final TileRendererFactory factory;

		public TileRenderingTuple(BSTile tile, TileRendererFactory factory) {
			this.tile = tile;
			this.factory = factory;
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(FBSR.class);

	private static final long TARGET_FILE_SIZE = 10 << 20; // 10MB
	private static final float ESTIMATED_JPG_PIXELS_PER_BYTE = 3.5f; // Based on measuring large JPG renders
	private static final long MAX_WORLD_RENDER_PIXELS = (long) (TARGET_FILE_SIZE * ESTIMATED_JPG_PIXELS_PER_BYTE);

	public static final Color GROUND_COLOR = new Color(40, 40, 40);
	public static final Color GRID_COLOR = new Color(0xffe6c0);

	private static final BasicStroke GRID_STROKE = new BasicStroke((float) (3 / FBSR.TILE_SIZE));

	private static volatile String version = null;

	private static final Map<String, Color> itemColorCache = new HashMap<>();

	public static final double TILE_SIZE = 64.0;

	private static volatile boolean initialized = false;

	private static void addToItemAmount(Map<String, Double> items, String itemName, double add) {
		double amount = items.getOrDefault(itemName, 0.0);
		amount += add;
		items.put(itemName, amount);
	}

	private static Rectangle2D.Double computeSpriteBounds(List<Renderer> renderers) {
		if (renderers.isEmpty()) {
			return new Rectangle2D.Double();
		}
		boolean first = true;
		double minX = 0, minY = 0, maxX = 0, maxY = 0;
		for (Renderer renderer : renderers) {
			MapRect3D bounds = renderer.bounds;
			if (first) {
				first = false;
				minX = bounds.x1;
				minY = bounds.y1 - bounds.height;
				maxX = bounds.x2;
				maxY = bounds.y2;
			} else {
				minX = Math.min(minX, bounds.x1);
				minY = Math.min(minY, bounds.y1 - bounds.height);
				maxX = Math.max(maxX, bounds.x2);
				maxY = Math.max(maxY, bounds.y2);
			}
		}
		return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
	}

	private static Rectangle2D.Double computeDrawBounds(List<Renderer> renderers) {
		if (renderers.isEmpty()) {
			return new Rectangle2D.Double();
		}
		boolean first = true;
		double minX = 0, minY = 0, maxX = 0, maxY = 0;
		for (Renderer renderer : renderers) {
			if (renderer.ignoreBoundsCalculation()) {
				continue;
			}
			MapRect3D bounds = renderer.bounds;
			if (first) {
				first = false;
				minX = bounds.x1;
				minY = bounds.y1 - bounds.height;
				maxX = bounds.x2;
				maxY = bounds.y2;
			} else {
				minX = Math.min(minX, bounds.x1);
				minY = Math.min(minY, bounds.y1 - bounds.height);
				maxX = Math.max(maxX, bounds.x2);
				maxY = Math.max(maxY, bounds.y2);
			}
		}
		return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
	}

	private static Rectangle2D.Double computeGroundBounds(List<Renderer> renderers) {
		if (renderers.isEmpty()) {
			return new Rectangle2D.Double();
		}
		boolean first = true;
		double minX = 0, minY = 0, maxX = 0, maxY = 0;
		for (Renderer renderer : renderers) {
			if (renderer.ignoreBoundsCalculation()) {
				continue;
			}
			MapRect3D bounds = renderer.bounds;
			if (first) {
				first = false;
				minX = bounds.x1;
				minY = bounds.y1;
				maxX = bounds.x2;
				maxY = bounds.y2;
			} else {
				minX = Math.min(minX, bounds.x1);
				minY = Math.min(minY, bounds.y1);
				maxX = Math.max(maxX, bounds.x2);
				maxY = Math.max(maxY, bounds.y2);
			}
		}
		return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
	}

	public static Map<String, Double> generateTotalItems(BSBlueprint blueprint) {

		Map<String, Double> ret = new LinkedHashMap<>();
		for (BSEntity entity : blueprint.entities) {
			String entityName = entity.name;
			EntityRendererFactory<BSEntity> entityFactory = FactorioManager.lookupEntityFactoryForName(entityName);
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

			Multiset<String> modules = RenderUtils.getModules(entity);
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
			Point2D.Double pos = new Point2D.Double(c.getRowKey() / 2.0 + 0.25, c.getColumnKey() / 2.0 + 0.25);
			LogisticGridCell cell = c.getValue();
			cell.getMove().ifPresent(d -> {
				map.getLogisticGridCell(d.offset(pos, 0.5)).filter(mc -> mc.acceptMoveFrom(d))
						.ifPresent(mc -> mc.addMovedFrom(d.back()));
			});
			cell.getWarps().ifPresent(l -> {
				for (Point2D.Double p : l) {
					map.getLogisticGridCell(p).ifPresent(mc -> mc.addWarpedFrom(pos));
				}
			});
		});
	}

	private static void populateTransitLogistics(WorldMap map, boolean populateInputs, boolean populateOutputs) {
		Table<Integer, Integer, LogisticGridCell> logisticGrid = map.getLogisticGrid();
		ArrayDeque<Entry<Point2D.Double, LogisticGridCell>> work = new ArrayDeque<>();

		if (populateOutputs) {
			logisticGrid.cellSet().stream().filter(c -> c.getValue().isTransitStart()).forEach(c -> {
				Set<String> outputs = c.getValue().getOutputs().get();
				for (String item : outputs) {
					work.add(new SimpleEntry<>(map.getLogisticCellPosition(c), c.getValue()));
					while (!work.isEmpty()) {
						Entry<Point2D.Double, LogisticGridCell> pair = work.pop();
						Point2D.Double cellPos = pair.getKey();
						LogisticGridCell cell = pair.getValue();
						if (cell.addTransit(item) && !cell.isBannedOutput(item)) {
							cell.getMove().ifPresent(d -> {
								Point2D.Double nextCellPos = d.offset(cellPos, 0.5);
								map.getLogisticGridCell(nextCellPos)
										.filter(nc -> !nc.isBlockTransit() && nc.acceptMoveFrom(d))
										.ifPresent(next -> work.add(new SimpleEntry<>(nextCellPos, next)));
							});
							cell.getWarps().ifPresent(l -> {
								for (Point2D.Double p : l) {
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
						Entry<Point2D.Double, LogisticGridCell> pair = work.pop();
						Point2D.Double cellPos = pair.getKey();
						LogisticGridCell cell = pair.getValue();
						if (cell.addTransit(item)) {
							cell.getMovedFrom().ifPresent(l -> {
								for (Direction d : l) {
									Point2D.Double nextCellPos = d.offset(cellPos, 0.5);
									map.getLogisticGridCell(nextCellPos).filter(nc -> !nc.isBlockTransit())
											.ifPresent(next -> work.add(new SimpleEntry<>(nextCellPos, next)));
								}
							});
							cell.getWarpedFrom().ifPresent(l -> {
								for (Point2D.Double p : l) {
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

		List<EntityRenderingTuple> entityRenderingTuples = new ArrayList<EntityRenderingTuple>();
		List<TileRenderingTuple> tileRenderingTuples = new ArrayList<TileRenderingTuple>();
		Map<Integer, EntityRenderingTuple> entityByNumber = new HashMap<>();

		for (BSMetaEntity metaEntity : blueprint.entities) {
			EntityRendererFactory<BSEntity> factory = FactorioManager.lookupEntityFactoryForName(metaEntity.name);
			BSEntity entity;
			try {
				if (metaEntity.isLegacy()) {
					entity = factory.parseEntity(metaEntity.getLegacy());
				} else {
					entity = factory.parseEntity(metaEntity.getJson());
				}
			} catch (Exception e) {
				metaEntity.setParseException(Optional.of(e));
				entity = metaEntity;
			}
			if (metaEntity.getParseException().isPresent()) {
				factory = new ErrorRendering(factory);
				reporting.addException(metaEntity.getParseException().get(), entity.name + " " + entity.entityNumber);
			}
			EntityRenderingTuple tuple = new EntityRenderingTuple(entity, factory);
			entityRenderingTuples.add(tuple);
			entityByNumber.put(entity.entityNumber, tuple);
		}
		for (BSTile tile : blueprint.tiles) {
			TileRendererFactory factory = FactorioManager.lookupTileFactoryForName(tile.name);
			TileRenderingTuple tuple = new TileRenderingTuple(tile, factory);
			tileRenderingTuples.add(tuple);
		}

		map.setFoundation(tileRenderingTuples.stream().anyMatch(t -> t.factory.getPrototype().isFoundation()));

		entityRenderingTuples.forEach(t -> {
			try {
				t.factory.populateWorldMap(map, t.entity);
			} catch (Exception e) {
				reporting.addException(e, t.factory.getClass().getSimpleName() + ", " + t.entity.name);
			}
		});
		tileRenderingTuples.forEach(t -> {
			try {
				t.factory.populateWorldMap(map, t.tile);
			} catch (Exception e) {
				reporting.addException(e, t.factory.getClass().getSimpleName() + ", " + t.tile.name);
			}
		});

		entityRenderingTuples.forEach(t -> {
			try {
				t.factory.populateLogistics(map, t.entity);
			} catch (Exception e) {
				reporting.addException(e, t.factory.getClass().getSimpleName() + ", " + t.entity.name);
			}
		});

		populateReverseLogistics(map);
		populateTransitLogistics(map, request.show.pathInputs, request.show.pathOutputs);

		populateRailBlocking(map);
		populateRailStationLogistics(map);

		List<Renderer> renderers = new ArrayList<>();
		Consumer register = r -> renderers.add((Renderer) r);

		TileRendererFactory.createAllRenderers(renderers::add, tileRenderingTuples);

		tileRenderingTuples.forEach(t -> {
			try {
				t.factory.createRenderers(register, map, t.tile);
			} catch (Exception e) {
				reporting.addException(e, t.factory.getClass().getSimpleName() + ", " + t.tile.name);
			}
		});

		entityRenderingTuples.forEach(t -> {
			try {
				t.factory.createRenderers(register, map, t.entity);
			} catch (Exception e) {
				reporting.addException(e, t.factory.getClass().getSimpleName() + ", " + t.entity.name);
			}
		});

		if (map.isAltMode()) {
			entityRenderingTuples.forEach(t -> {
				try {
					t.factory.createModuleIcons(register, map, t.entity);
				} catch (Exception e) {
					reporting.addException(e, t.factory.getClass().getSimpleName() + ", " + t.entity.name);
				}
			});
		}

		Map<Integer, Double> connectorOrientations = new HashMap<>();
		int[] wireEntityNumbers = blueprint.wires.stream()
				.flatMapToInt(w -> IntStream.of(w.firstEntityNumber, w.secondEntityNumber)).distinct().toArray();
		for (int entityNumber : wireEntityNumbers) {
			EntityRenderingTuple tuple = entityByNumber.get(entityNumber);
			List<EntityRenderingTuple> wired = blueprint.wires.stream().flatMapToInt(w -> {
				if (w.firstEntityNumber == entityNumber) {
					return IntStream.of(w.secondEntityNumber);
				} else if (w.secondEntityNumber == entityNumber) {
					return IntStream.of(w.firstEntityNumber);
				} else {
					return IntStream.of();
				}
			}).mapToObj(entityByNumber::get).collect(Collectors.toList());

			double orientation = tuple.factory.initWireConnector(register, tuple.entity, wired);
			connectorOrientations.put(entityNumber, orientation);
		}

		for (BSWire wire : blueprint.wires) {
			try {
				EntityRenderingTuple first = entityByNumber.get(wire.firstEntityNumber);
				EntityRenderingTuple second = entityByNumber.get(wire.secondEntityNumber);

				double orientation1 = connectorOrientations.get(wire.firstEntityNumber);
				double orientation2 = connectorOrientations.get(wire.secondEntityNumber);

				Optional<WirePoint> firstPoint = first.factory.createWirePoint(register,
						first.entity.position.createPoint(), orientation1, wire.firstWireConnectorId);
				Optional<WirePoint> secondPoint = second.factory.createWirePoint(register,
						second.entity.position.createPoint(), orientation2, wire.secondWireConnectorId);

				if (!firstPoint.isPresent() || !secondPoint.isPresent()) {
					continue;// Probably something modded
				}

				renderers.add(RenderUtils.createWireRenderer(firstPoint.get().getPosition(),
						secondPoint.get().getPosition(), firstPoint.get().getColor().getColor(),
						firstPoint.get().getShadow(), secondPoint.get().getShadow()));
			} catch (Exception e) {
				reporting.addException(e, "Wire " + wire.firstEntityNumber + ", " + wire.firstWireConnectorId + ", "
						+ wire.secondEntityNumber + ", " + wire.secondWireConnectorId);
			}
		}

		showLogisticGrid(register, map, request.debug.pathItems);
		showRailLogistics(register, map, request.debug.pathRails);

		if (request.debug.entityPlacement) {
			entityRenderingTuples.forEach(t -> {
				Point2D.Double pos = t.entity.position.createPoint();
				renderers.add(new Renderer(Layer.DEBUG_P, pos, true) {
					@Override
					public void render(Graphics2D g) {
						g.setColor(Color.cyan);
						g.fill(new Ellipse2D.Double(pos.x - 0.1, pos.y - 0.1, 0.2, 0.2));
						Stroke ps = g.getStroke();
						g.setStroke(new BasicStroke(3f / (float) TILE_SIZE));
						g.setColor(Color.green);
						g.draw(new Line2D.Double(pos, t.entity.direction.offset(pos, 0.3)));
						g.setStroke(ps);
					}
				});
			});
			tileRenderingTuples.forEach(t -> {
				Point2D.Double pos = t.tile.position.createPoint();
				renderers.add(new Renderer(Layer.DEBUG_P, pos, true) {

					@Override
					public void render(Graphics2D g) {
						g.setColor(Color.cyan);
						g.fill(new Ellipse2D.Double(pos.x - 0.1, pos.y - 0.1, 0.2, 0.2));
					}
				});
			});
		}

		double gridPadding = (!request.getGridLines().isEmpty() && request.show.gridNumbers) ? 1 : 0;
		double gridRound = (!request.getGridLines().isEmpty() && request.show.gridNumbers) ? 0.6 : 0.2;
		double worldPadding = 0.5;

		Rectangle2D.Double visualBounds = computeDrawBounds(renderers);
		visualBounds.setFrameFromDiagonal(Math.floor(visualBounds.getMinX() + 0.4),
				Math.floor(visualBounds.getMinY() + 0.4), Math.ceil(visualBounds.getMaxX() - 0.4),
				Math.ceil(visualBounds.getMaxY() - 0.4));
		Rectangle2D.Double gridBounds = computeGroundBounds(renderers);
		gridBounds.setFrameFromDiagonal(Math.floor(gridBounds.getMinX() + 0.4) - gridPadding,
				Math.floor(gridBounds.getMinY() + 0.4) - gridPadding,
				Math.ceil(gridBounds.getMaxX() - 0.4) + gridPadding,
				Math.ceil(gridBounds.getMaxY() - 0.4) + gridPadding);

		Rectangle2D.Double worldBounds = new Rectangle2D.Double();
		worldBounds.setFrameFromDiagonal(//
				Math.min(visualBounds.getMinX(), gridBounds.getMinX()) - worldPadding, //
				Math.min(visualBounds.getMinY(), gridBounds.getMinY()) - worldPadding, //
				Math.max(visualBounds.getMaxX(), gridBounds.getMaxX()) + worldPadding, //
				Math.max(visualBounds.getMaxY(), gridBounds.getMaxY()) + worldPadding);//

		double worldRenderScale = 1;

		boolean gridPlatformMode = map.isFoundation() && !request.show.gridNumbers;

		// Max scale limit
		if (request.getMaxScale().isPresent()) {
			worldRenderScale = request.getMaxScale().getAsDouble();
		}

		// Shrink down the scale to fit the max requirements
		int maxWidthPixels = request.getMaxWidth().orElse(Integer.MAX_VALUE);
		int maxHeightPixels = request.getMaxHeight().orElse(Integer.MAX_VALUE);
		long maxPixels = Math.min(MAX_WORLD_RENDER_PIXELS, (long) maxWidthPixels * (long) maxHeightPixels);

		if ((worldBounds.getWidth() * worldRenderScale * TILE_SIZE) > maxWidthPixels) {
			worldRenderScale *= (maxWidthPixels / (worldBounds.getWidth() * worldRenderScale * TILE_SIZE));
		}
		if ((worldBounds.getHeight() * worldRenderScale * TILE_SIZE) > maxHeightPixels) {
			worldRenderScale *= (maxHeightPixels / (worldBounds.getHeight() * worldRenderScale * TILE_SIZE));
		}
		if ((worldBounds.getWidth() * worldRenderScale * TILE_SIZE)
				* (worldBounds.getHeight() * worldRenderScale * TILE_SIZE) > maxPixels) {
			worldRenderScale *= Math.sqrt(maxPixels / ((worldBounds.getWidth() * worldRenderScale * TILE_SIZE)
					* (worldBounds.getHeight() * worldRenderScale * TILE_SIZE)));
		}

		// Expand the world to fit the min requirements
		int minWidthPixels = request.getMinWidth().orElse(0);
		int minHeightPixels = request.getMinHeight().orElse(0);

		if ((worldBounds.getWidth() * worldRenderScale * TILE_SIZE) < minWidthPixels) {
			double padding = (minWidthPixels - (worldBounds.getWidth() * worldRenderScale * TILE_SIZE))
					/ (worldRenderScale * TILE_SIZE);
			worldBounds.x -= padding / 2.0;
			worldBounds.width += padding;
		}
		if ((worldBounds.getHeight() * worldRenderScale * TILE_SIZE) < minHeightPixels) {
			double padding = (minHeightPixels - (worldBounds.getHeight() * worldRenderScale * TILE_SIZE))
					/ (worldRenderScale * TILE_SIZE);
			worldBounds.y -= padding / 2.0;
			worldBounds.height += padding;
		}

//		int imageWidth = Math.max(minWidthPixels,
//				Math.min(maxWidthPixels, (int) (worldBounds.getWidth() * worldRenderScale * TILE_SIZE)));
//		int imageHeight = Math.max(minHeightPixels,
//				Math.min(maxHeightPixels, (int) (worldBounds.getHeight() * worldRenderScale * TILE_SIZE)));
		int imageWidth = Math.max(minWidthPixels,
				Math.min(maxWidthPixels, (int) Math.round(worldBounds.getWidth() * worldRenderScale * TILE_SIZE)));
		int imageHeight = Math.max(minHeightPixels,
				Math.min(maxHeightPixels, (int) Math.round(worldBounds.getHeight() * worldRenderScale * TILE_SIZE)));
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

		g.scale(image.getWidth() / worldBounds.getWidth(), image.getHeight() / worldBounds.getHeight());
		g.translate(-worldBounds.getX(), -worldBounds.getY());
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
			g.fill(worldBounds);
		}

		boolean gridTooSmall = (1 / worldRenderScale) > 5;

		Layer gridLayer;
		if (request.show.gridAboveBelts) {
			gridLayer = Layer.GRID_ABOVE_BELTS;
		} else {
			gridLayer = Layer.GRID;
		}

		// Grid Lines
		if (request.getGridLines().isPresent() && !gridTooSmall) {
			if (gridPlatformMode) {
				renderers.add(new Renderer(gridLayer, new MapRect3D(gridBounds, 0), true) {
					@Override
					public void render(Graphics2D g) throws Exception {
						g.setStroke(GRID_STROKE);
						g.setColor(request.getGridLines().get());
						Rectangle2D.Double rect = new Rectangle2D.Double(0, 0, 1, 1);
						for (TileRenderingTuple tuple : tileRenderingTuples) {
							BSPosition pos = tuple.tile.position;
							rect.x = pos.x;
							rect.y = pos.y;
							g.draw(rect);
						}
					}
				});
			} else {
				renderers.add(new Renderer(gridLayer, new MapRect3D(gridBounds, 0), true) {
					@Override
					public void render(Graphics2D g) throws Exception {
						g.setStroke(GRID_STROKE);
						g.setColor(request.getGridLines().get());
						for (double x = Math.round(gridBounds.getMinX()) + 1; x <= gridBounds.getMaxX() - 1; x++) {
							g.draw(new Line2D.Double(x, gridBounds.getMinY(), x, gridBounds.getMaxY()));
						}
						for (double y = Math.round(gridBounds.getMinY()) + 1; y <= gridBounds.getMaxY() - 1; y++) {
							g.draw(new Line2D.Double(gridBounds.getMinX(), y, gridBounds.getMaxX(), y));
						}
						g.draw(new RoundRectangle2D.Double(gridBounds.x, gridBounds.y, gridBounds.width,
								gridBounds.height, gridRound, gridRound));
					}
				});
			}
		}

		renderers.stream().filter(r -> r instanceof EntityRenderer).map(r -> (EntityRenderer) r).forEach(r -> {
			try {
				r.renderShadows(shadowG);
			} catch (Exception e) {
				reporting.addException(e);
			}
		});
		shadowG.dispose();
		RenderUtils.halveAlpha(shadowImage);

		renderers.add(new Renderer(Layer.SHADOW_BUFFER, new MapRect3D(worldBounds, 0), true) {
			@Override
			public void render(Graphics2D g) throws Exception {
				AffineTransform tempXform = g.getTransform();
				g.setTransform(noXform);
				g.drawImage(shadowImage, 0, 0, null);

				g.setTransform(tempXform);
			}
		});

		boolean debugBounds = request.debug.entityPlacement;
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

				if (debugBounds) {
					g.setStroke(new BasicStroke(1f / (float) TILE_SIZE));
					g.setColor(r.ignoreBoundsCalculation() ? Color.gray : Color.magenta);
					MapRect3D b = r.bounds;
					g.draw(new Rectangle2D.Double(b.x1, b.y1 - b.height, b.x2 - b.x1, b.y2 - b.y1 + b.height));
					if (b.height > 0) {
						g.setColor(g.getColor().darker());
						g.draw(new Line2D.Double(b.x1, b.y1, b.x2, b.y1));
					}
				}
			} catch (Exception e) {
				reporting.addException(e);
			}
		});
		g.setTransform(worldXform);

		// Grid Numbers
		if (request.getGridLines().isPresent() && request.show.gridNumbers && !gridTooSmall) {
			g.setColor(request.getGridLines().get());
			g.setFont(GUIStyle.FONT_BP_REGULAR.deriveFont(0.6f));
			float tx = 0.18f;
			float ty = 0.68f;
			Color gridColor = request.getGridLines().get();
			g.setColor(gridColor);
			for (double x = Math.round(gridBounds.getMinX()) + 1, i = 1; x <= gridBounds.getMaxX() - 2; x++, i++) {
				String strNum = String.format("%02d", (int) Math.round(i) % 100);
				float x1 = (float) x + tx;
				float y1 = (float) (gridBounds.getMaxY() - 1 + ty);
				float y2 = (float) (gridBounds.getMinY() + ty);
				g.drawString(strNum, x1, y1);
				g.drawString(strNum, x1, y2);
			}
			for (double y = Math.round(gridBounds.getMinY()) + 1, i = 1; y <= gridBounds.getMaxY() - 2; y++, i++) {
				String strNum = String.format("%02d", (int) Math.round(i) % 100);
				float x1 = (float) (gridBounds.getMaxX() - 1 + tx);
				float y1 = (float) y + ty;
				float x2 = (float) (gridBounds.getMinX() + tx);
				g.drawString(strNum, x1, y1);
				g.drawString(strNum, x2, y1);
			}
		}

		g.dispose();

		long endMillis = System.currentTimeMillis();
		LOGGER.info("\tRender Time {} ms", endMillis - startMillis);

		RenderResult result = new RenderResult(image, endMillis - startMillis, worldRenderScale);
		return result;
	}

	private static void showLogisticGrid(Consumer<Renderer> register, WorldMap map, boolean debug) {
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

	private static void showRailLogistics(Consumer<Renderer> register, WorldMap map, boolean debug) {
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
