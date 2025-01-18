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

import com.demod.dcba.CommandReporting;
import com.demod.dcba.CommandReporting.Level;
import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.ModInfo;
import com.demod.factorio.TotalRawCalculator;
import com.demod.factorio.Utils;
import com.demod.factorio.prototype.DataPrototype;
import com.demod.factorio.prototype.ItemPrototype;
import com.demod.factorio.prototype.RecipePrototype;
import com.demod.fbsr.WirePoints.WirePoint;
import com.demod.fbsr.WorldMap.RailEdge;
import com.demod.fbsr.WorldMap.RailNode;
import com.demod.fbsr.bs.BSBlueprint;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.BSTile;
import com.demod.fbsr.bs.BSWire;
import com.demod.fbsr.entity.ErrorRendering;
import com.demod.fbsr.gui.GUIStyle;
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

	private static final int MAX_WORLD_RENDER_WIDTH = 8000;
	private static final int MAX_WORLD_RENDER_HEIGHT = 8000;
	private static final long MAX_WORLD_RENDER_PIXELS = 3000 * 3000;

	public static final Color GROUND_COLOR = new Color(40, 40, 40);
	public static final Color GRID_COLOR = GUIStyle.FONT_BP_COLOR.darker().darker();

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

	private static Rectangle2D.Double computeBounds(List<Renderer> renderers, boolean includeIgnoredBounds) {
		if (renderers.isEmpty()) {
			return new Rectangle2D.Double();
		}
		boolean first = true;
		double minX = 0, minY = 0, maxX = 0, maxY = 0;
		for (Renderer renderer : renderers) {
			if (!includeIgnoredBounds && renderer.ignoreBoundsCalculation()) {
				continue;
			}
			Rectangle2D.Double bounds = renderer.bounds;
			if (first) {
				first = false;
				minX = bounds.getMinX();
				minY = bounds.getMinY();
				maxX = bounds.getMaxX();
				maxY = bounds.getMaxY();
			} else {
				minX = Math.min(minX, bounds.getMinX());
				minY = Math.min(minY, bounds.getMinY());
				maxX = Math.max(maxX, bounds.getMaxX());
				maxY = Math.max(maxY, bounds.getMaxY());
			}
		}
		return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
	}

	public static Map<String, Double> generateSummedTotalItems(BSBlueprint blueprint) {
		Map<String, Double> ret = new LinkedHashMap<>();
		if (!blueprint.entities.isEmpty())
			ret.put("Entities", (double) blueprint.entities.size());

		DataTable table = FactorioData.getTable();

		for (BSEntity entity : blueprint.entities) {
			Multiset<String> modules = RenderUtils.getModules(entity, table);
			for (Multiset.Entry<String> entry : modules.entrySet()) {
				addToItemAmount(ret, "Modules", entry.getCount());
			}
		}
		for (BSTile tile : blueprint.tiles) {
			String itemName = tile.name;
			// TODO hard-coded
			if (itemName.startsWith("hazard-concrete")) {
				itemName = "hazard-concrete";
			}
			if (itemName.startsWith("refined-hazard-concrete")) {
				itemName = "refined-hazard-concrete";
			}
			if (itemName.equals("stone-path")) {
				itemName = "stone-brick";
			}
			if (itemName.equals("grass-1")) {
				itemName = "landfill";
			}
			if (!table.getItem(itemName).isPresent()) {
				System.err.println("MISSING TILE ITEM: " + itemName);
				continue;
			}
			addToItemAmount(ret, "Tiles", 1);
		}

		return ret;
	}

	public static Map<String, Double> generateTotalItems(BSBlueprint blueprint) {
		DataTable table = FactorioData.getTable();

		Map<String, Double> ret = new LinkedHashMap<>();
		for (BSEntity entity : blueprint.entities) {
			String entityName = entity.name;
			List<ItemPrototype> items = table.getItemsForEntity(entityName);
			if (items.isEmpty()) {
				// reporting.addWarning("Cannot find items for entity: " +
				// entity.getName());
				continue;
			}
			items.forEach(i -> {
				addToItemAmount(ret, i.getName(), 1);
			});

			Multiset<String> modules = RenderUtils.getModules(entity, table);
			for (Multiset.Entry<String> entry : modules.entrySet()) {
				addToItemAmount(ret, entry.getElement(), entry.getCount());
			}
		}
		for (BSTile tile : blueprint.tiles) {
			String itemName = tile.name;
			// TODO hard-coded
			if (itemName.startsWith("hazard-concrete")) {
				itemName = "hazard-concrete";
			}
			if (itemName.startsWith("refined-hazard-concrete")) {
				itemName = "refined-hazard-concrete";
			}
			if (itemName.equals("stone-path")) {
				itemName = "stone-brick";
			}
			if (itemName.equals("grass-1")) {
				itemName = "landfill";
			}
			if (!table.getItem(itemName).isPresent()) {
				System.err.println("MISSING TILE ITEM: " + itemName);
				continue;
			}
			addToItemAmount(ret, itemName, 1);
		}
		return ret;
	}

	public static Map<String, Double> generateTotalRawItems(Map<String, Double> totalItems) {
		DataTable table = FactorioData.getTable();
		Map<String, RecipePrototype> recipes = table.getRecipes();
		Map<String, Double> ret = new LinkedHashMap<>();
		TotalRawCalculator calculator = new TotalRawCalculator(recipes);
		for (Entry<String, Double> entry : totalItems.entrySet()) {
			String recipeName = entry.getKey();
			double recipeAmount = entry.getValue();
			table.getRecipe(recipeName).ifPresent(r -> {
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

	private static Color getItemLogisticColor(DataTable table, String itemName) {
		return itemColorCache.computeIfAbsent(itemName, k -> {
			Optional<ItemPrototype> optProto = table.getItem(k);
			if (!optProto.isPresent()) {
				System.err.println("ITEM MISSING FOR LOGISTICS: " + k);
				return Color.MAGENTA;
			}
			DataPrototype prototype = optProto.get();
			BufferedImage image = FactorioData.getIcon(prototype);
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
			try {
				baseInfo = new ModInfo(Utils.readJsonFromStream(
						new FileInputStream(new File(FactorioData.factorio, "data/base/info.json"))));
				version = baseInfo.getVersion();
			} catch (JSONException | IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
		return version;
	}

	public static synchronized void initialize() throws JSONException, IOException {
		if (initialized) {
			return;
		}
		DataTable table = FactorioData.getTable();
		EntityRendererFactory.initPrototypes(table);
		TileRendererFactory.initPrototypes(table);
		initialized = true;
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

		System.out.println("Rendering " + blueprint.label.orElse("(No Name)"));
		long startMillis = System.currentTimeMillis();

		DataTable table = FactorioData.getTable();
		WorldMap map = new WorldMap();

		map.setAltMode(request.show.altMode);

		List<EntityRenderingTuple> entityRenderingTuples = new ArrayList<EntityRenderingTuple>();
		List<TileRenderingTuple> tileRenderingTuples = new ArrayList<TileRenderingTuple>();
		Map<Integer, EntityRenderingTuple> entityByNumber = new HashMap<>();

		for (BSEntity entity : blueprint.entities) {
			EntityRendererFactory<BSEntity> factory = EntityRendererFactory.forName(entity.name);
			try {
				entity = factory.parseEntity(entity.getJson());
			} catch (Exception e) {
				entity.setParseException(Optional.of(e));
			}
			if (entity.getParseException().isPresent()) {
				factory = new ErrorRendering(factory);
				reporting.addException(entity.getParseException().get(), entity.name + " " + entity.entityNumber);
			}
			EntityRenderingTuple tuple = new EntityRenderingTuple(entity, factory);
			entityRenderingTuples.add(tuple);
			entityByNumber.put(entity.entityNumber, tuple);
		}
		for (BSTile tile : blueprint.tiles) {
			TileRendererFactory factory = TileRendererFactory.forName(tile.name);
			TileRenderingTuple tuple = new TileRenderingTuple(tile, factory);
			tileRenderingTuples.add(tuple);
		}

		entityRenderingTuples.forEach(t -> {
			try {
				t.factory.populateWorldMap(map, table, t.entity);
			} catch (Exception e) {
				reporting.addException(e, t.factory.getClass().getSimpleName() + ", " + t.entity.name);
			}
		});
		tileRenderingTuples.forEach(t -> {
			try {
				t.factory.populateWorldMap(map, table, t.tile);
			} catch (Exception e) {
				reporting.addException(e, t.factory.getClass().getSimpleName() + ", " + t.tile.name);
			}
		});

		entityRenderingTuples.forEach(t -> {
			try {
				t.factory.populateLogistics(map, table, t.entity);
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
				t.factory.createRenderers(register, map, table, t.tile);
			} catch (Exception e) {
				reporting.addException(e, t.factory.getClass().getSimpleName() + ", " + t.tile.name);
			}
		});

		entityRenderingTuples.forEach(t -> {
			try {
				t.factory.createRenderers(register, map, table, t.entity);
			} catch (Exception e) {
				reporting.addException(e, t.factory.getClass().getSimpleName() + ", " + t.entity.name);
			}
		});

		if (map.isAltMode()) {
			entityRenderingTuples.forEach(t -> {
				try {
					t.factory.createModuleIcons(register, map, table, t.entity);
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

		showLogisticGrid(register, table, map, request.debug.pathItems);
		showRailLogistics(register, table, map, request.debug.pathRails);

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

		double gridPadding = (!request.getGridLines().isEmpty() && request.show.gridNumbers) ? 1.25 : 0.25;

		Rectangle2D.Double worldBounds = computeBounds(renderers, true);
		worldBounds.setFrameFromDiagonal(Math.floor(worldBounds.getMinX() + 0.4) - gridPadding,
				Math.floor(worldBounds.getMinY() + 0.4) - gridPadding,
				Math.ceil(worldBounds.getMaxX() - 0.4) + gridPadding,
				Math.ceil(worldBounds.getMaxY() - 0.4) + gridPadding);
		Rectangle2D.Double gridBounds = computeBounds(renderers, false);

		double worldRenderScale = 1;

		// Max scale limit
		if (request.getMaxScale().isPresent()) {
			worldRenderScale = request.getMaxScale().getAsDouble();
		}

		// Shrink down the scale to fit the max requirements
		int maxWidthPixels = request.getMaxWidth().orElse(MAX_WORLD_RENDER_WIDTH);
		int maxHeightPixels = request.getMaxHeight().orElse(MAX_WORLD_RENDER_HEIGHT);
		long maxPixels = Math.min(MAX_WORLD_RENDER_PIXELS, (long) maxWidthPixels * (long) maxHeightPixels);

		if ((worldBounds.getWidth() * worldRenderScale * TILE_SIZE) > maxWidthPixels) {
			worldRenderScale *= (maxWidthPixels / (worldBounds.getWidth() * worldRenderScale * TILE_SIZE));
		}
		if ((worldBounds.getHeight() * worldRenderScale * TILE_SIZE) > maxHeightPixels) {
			worldRenderScale *= (maxHeightPixels / (worldBounds.getHeight() * worldRenderScale * TILE_SIZE));
		}
		if ((worldBounds.getWidth() * worldBounds.getHeight() * worldRenderScale * TILE_SIZE) > maxPixels) {
			worldRenderScale *= (maxPixels
					/ (worldBounds.getWidth() * worldBounds.getHeight() * worldRenderScale * TILE_SIZE));
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

		int imageWidth = Math.max(minWidthPixels,
				Math.min(maxWidthPixels, (int) (worldBounds.getWidth() * worldRenderScale * TILE_SIZE)));
		int imageHeight = Math.max(minHeightPixels,
				Math.min(maxHeightPixels, (int) (worldBounds.getHeight() * worldRenderScale * TILE_SIZE)));
		System.out.println("\t" + imageWidth + "x" + imageHeight + " (" + worldRenderScale + ")");

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
			renderers.add(new Renderer(gridLayer, gridBounds, true) {
				@Override
				public void render(Graphics2D g) throws Exception {
					g.setStroke(GRID_STROKE);
					g.setColor(request.getGridLines().get());
					for (double x = Math.round(gridBounds.getMinX()); x <= gridBounds.getMaxX(); x++) {
						g.draw(new Line2D.Double(x, gridBounds.getMinY(), x, gridBounds.getMaxY()));
					}
					for (double y = Math.round(gridBounds.getMinY()); y <= gridBounds.getMaxY(); y++) {
						g.draw(new Line2D.Double(gridBounds.getMinX(), y, gridBounds.getMaxX(), y));
					}
				}
			});
		}

		renderers.stream().filter(r1 -> r1 instanceof EntityRenderer).map(r3 -> (EntityRenderer) r3).forEach(r2 -> {
			try {
				r2.renderShadows(shadowG);
			} catch (Exception e) {
				reporting.addException(e);
			}
		});
		shadowG.dispose();
		RenderUtils.halveAlpha(shadowImage);

		renderers.add(new Renderer(Layer.SHADOW_BUFFER, worldBounds, true) {
			@Override
			public void render(Graphics2D g) throws Exception {
				AffineTransform tempXform = g.getTransform();
				g.setTransform(noXform);
				g.drawImage(shadowImage, 0, 0, null);

				g.setTransform(tempXform);
			}
		});

		boolean debugBounds = request.debug.entityPlacement;
		renderers.stream().sorted((r11, r21) -> {
			int ret;

			ret = r11.getLayer().compareTo(r21.getLayer());
			if (ret != 0) {
				return ret;
			}

			Rectangle2D.Double b1 = r11.getBounds();
			Rectangle2D.Double b2 = r21.getBounds();

			ret = Double.compare(b1.getMinY(), b2.getMinY());
			if (ret != 0) {
				return ret;
			}

			ret = Double.compare(b1.getMinX(), b2.getMinX());
			if (ret != 0) {
				return ret;
			}

			ret = r11.getLayer().compareTo(r21.getLayer());
			return ret;
		}).forEach(r4 -> {
			try {
				r4.render(g);

				if (debugBounds) {
					g.setStroke(new BasicStroke(1f / (float) TILE_SIZE));
					g.setColor(r4.ignoreBoundsCalculation() ? Color.gray : Color.magenta);
					g.draw(r4.bounds);
				}
			} catch (Exception e) {
				reporting.addException(e);
			}
		});
		g.setTransform(worldXform);

		// Grid Numbers
		if (request.getGridLines().isPresent() && request.show.gridNumbers && !gridTooSmall) {
			g.setColor(request.getGridLines().get());
			g.setFont(new Font("Monospaced", Font.BOLD, 1).deriveFont(0.6f));
			for (double x = Math.round(gridBounds.getMinX()) + 1, i = 1; x <= gridBounds.getMaxX() - 2; x++, i++) {
				g.drawString(String.format("%02d", (int) Math.round(i) % 100), (float) x + 0.2f,
						(float) (gridBounds.getMaxY() - 1 + 0.65f));
				g.drawString(String.format("%02d", (int) Math.round(i) % 100), (float) x + 0.2f,
						(float) (gridBounds.getMinY() + 0.65f));
			}
			for (double y = Math.round(gridBounds.getMinY()) + 1, i = 1; y <= gridBounds.getMaxY() - 2; y++, i++) {
				g.drawString(String.format("%02d", (int) Math.round(i) % 100),
						(float) (gridBounds.getMaxX() - 1 + 0.2f), (float) y + 0.65f);
				g.drawString(String.format("%02d", (int) Math.round(i) % 100), (float) (gridBounds.getMinX() + 0.2f),
						(float) y + 0.65f);
			}
		}

		Level level = reporting.getLevel();
		if (level != Level.INFO) {
			g.setTransform(worldXform);
			g.setStroke(GRID_STROKE);
			g.setColor(level.getColor().darker());
			g.draw(worldBounds);
		}

		g.dispose();

		long endMillis = System.currentTimeMillis();
		System.out.println("\tRender Time " + (endMillis - startMillis) + " ms");

		RenderResult result = new RenderResult(image, endMillis - startMillis, worldRenderScale);
		return result;
	}

	private static void showLogisticGrid(Consumer<Renderer> register, DataTable table, WorldMap map, boolean debug) {
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
										g.setColor(RenderUtils.withAlpha(getItemLogisticColor(table, itemName),
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

	private static void showRailLogistics(Consumer<Renderer> register, DataTable table, WorldMap map, boolean debug) {
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
