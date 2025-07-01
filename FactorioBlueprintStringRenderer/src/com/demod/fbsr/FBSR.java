package com.demod.fbsr;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.dcba.CommandReporting;
import com.demod.factorio.DataTable;
import com.demod.factorio.ItemToPlace;
import com.demod.factorio.ModInfo;
import com.demod.factorio.TotalRawCalculator;
import com.demod.factorio.Utils;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.factorio.prototype.ItemPrototype;
import com.demod.factorio.prototype.RecipePrototype;
import com.demod.factorio.prototype.TilePrototype;
import com.demod.fbsr.Profile.ProfileStatus;
import com.demod.fbsr.WirePoints.WirePoint;
import com.demod.fbsr.bs.BSBlueprint;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.BSItemStack;
import com.demod.fbsr.bs.BSItemStackItem;
import com.demod.fbsr.bs.BSItemWithQualityID;
import com.demod.fbsr.bs.BSMetaEntity;
import com.demod.fbsr.bs.BSPosition;
import com.demod.fbsr.bs.BSTile;
import com.demod.fbsr.bs.BSWire;
import com.demod.fbsr.entity.ErrorRendering;
import com.demod.fbsr.gui.GUIStyle;
import com.demod.fbsr.map.MapBounded;
import com.demod.fbsr.map.MapDebug;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapEntity.EntityModule;
import com.demod.fbsr.map.MapFoundationGrid;
import com.demod.fbsr.map.MapGrid;
import com.demod.fbsr.map.MapItemLogistics;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRailLogistics;
import com.demod.fbsr.map.MapRect;
import com.demod.fbsr.map.MapRect3D;
import com.demod.fbsr.map.MapRenderable;
import com.demod.fbsr.map.MapSnapToGrid;
import com.demod.fbsr.map.MapText;
import com.demod.fbsr.map.MapTile;
import com.demod.fbsr.map.MapWire;
import com.demod.fbsr.map.MapWireShadow;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Table;

public class FBSR {
	private static final Logger LOGGER = LoggerFactory.getLogger(FBSR.class);

	private static final long TARGET_FILE_SIZE = 10 << 20; // 10MB
	private static final float ESTIMATED_JPG_PIXELS_PER_BYTE = 3.5f; // Based on measuring large JPG renders
	private static final long MAX_WORLD_RENDER_PIXELS = (long) (TARGET_FILE_SIZE * ESTIMATED_JPG_PIXELS_PER_BYTE);

	public static final Color GROUND_COLOR = new Color(40, 40, 40);
	public static final Color GRID_COLOR = new Color(0xffe6c0).darker().darker();

	private static volatile String version = null;

	public static final double TILE_SIZE = 64.0;

	private static volatile boolean initialized = false;

	private static FactorioManager factorioManager;
	private static GUIStyle guiStyle;
	private static IconManager iconManager;

	private static final ExecutorService executor = Executors.newWorkStealingPool();

	private static class ImageRenderer implements Callable<RenderResult> {
		private final RenderRequest request;

		private CommandReporting reporting;
		private BSBlueprint blueprint;

		private List<MapEntity> mapEntities;
		private List<MapTile> mapTiles;
		private Map<Integer, MapEntity> mapEntityByNumber;
		private Multiset<String> unknownNames;

		private WorldMap map;

		private ListMultimap<Layer, MapRenderable> renderBuckets;
		private Consumer<MapRenderable> register;

		private Rectangle2D.Double screenBounds;
		private int imageWidth;
		private int imageHeight;
		private double worldRenderScale;

		private BufferedImage image;

		public ImageRenderer(RenderRequest request) {
			this.request = request;
		}

		@Override
		public RenderResult call() {
			blueprint = request.getBlueprint();
			reporting = request.getReporting();
			LOGGER.info("Rendering {} {}", blueprint.label.orElse("Untitled Blueprint"), blueprint.version);
			long startMillis = System.currentTimeMillis();

			parseBlueprint();

			populateMap();

			createRenderers();

			calculateBounds();

			LOGGER.info("\t{}x{} ({})", imageWidth, imageHeight, worldRenderScale);

			renderImage();

			long endMillis = System.currentTimeMillis();
			LOGGER.info("\tRender Time {} ms", endMillis - startMillis);
			return new RenderResult(request, image, endMillis - startMillis, worldRenderScale, unknownNames);
		}

		private void parseBlueprint() {
			mapEntities = new ArrayList<MapEntity>();
			mapTiles = new ArrayList<MapTile>();
			mapEntityByNumber = new HashMap<>();
			unknownNames = LinkedHashMultiset.create();

			ModdingResolver resolver = ModdingResolver.byBlueprintBiases(factorioManager, blueprint);

			for (BSMetaEntity metaEntity : blueprint.entities) {
				EntityRendererFactory factory = resolver.resolveFactoryEntityName(metaEntity.name);
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
					reporting.addException(metaEntity.getParseException().get(),
							entity.name + " " + entity.entityNumber);
				}
				MapEntity mapEntity = new MapEntity(entity, factory);
				mapEntities.add(mapEntity);
				mapEntityByNumber.put(entity.entityNumber, mapEntity);
				if (factory.isUnknown()) {
					unknownNames.add(metaEntity.name);
				}
			}
			for (BSTile tile : blueprint.tiles) {
				TileRendererFactory factory = resolver.resolveFactoryTileName(tile.name);
				MapTile mapTile = new MapTile(tile, factory);
				mapTiles.add(mapTile);
				if (factory.isUnknown()) {
					unknownNames.add(tile.name);
				}
			}

			mapEntities.sort(Comparator.comparing((MapEntity r) -> r.getPosition().getYFP())
					.thenComparing(r -> r.getPosition().getXFP()));
			mapTiles.sort(Comparator.comparing((MapTile r) -> r.getPosition().getYFP())
					.thenComparing(r -> r.getPosition().getXFP()));
		}

		private void populateMap() {
			map = new WorldMap();

			map.setAltMode(request.show.altMode);

			map.setFoundation(mapTiles.stream().anyMatch(t -> t.getFactory().getPrototype().isFoundation()));

			mapEntities.forEach(t -> {
				try {
					t.getFactory().populateWorldMap(map, t);
				} catch (Exception e) {
					reporting.addException(e,
							t.getFactory().getClass().getSimpleName() + ", " + t.fromBlueprint().name);
				}
			});
			mapTiles.forEach(t -> {
				try {
					t.getFactory().populateWorldMap(map, t);
				} catch (Exception e) {
					reporting.addException(e,
							t.getFactory().getClass().getSimpleName() + ", " + t.fromBlueprint().name);
				}
			});

			mapEntities.forEach(t -> {
				try {
					t.getFactory().populateLogistics(map, t);
				} catch (Exception e) {
					reporting.addException(e,
							t.getFactory().getClass().getSimpleName() + ", " + t.fromBlueprint().name);
				}
			});

			populateReverseLogistics(map);
			populateTransitLogistics(map, request.show.pathInputs, request.show.pathOutputs);

			populateRailBlocking(map, false);
			populateRailBlocking(map, true);
			populateRailStationLogistics(map);
		}

		private void createRenderers() {
			renderBuckets = MultimapBuilder.enumKeys(Layer.class).arrayListValues().build();
			register = r -> renderBuckets.put(r.getLayer(), r);

			TileRendererFactory.createAllRenderers(register, mapTiles);

			mapTiles.forEach(t -> {
				try {
					t.getFactory().createRenderers(register, map, t);
				} catch (Exception e) {
					reporting.addException(e,
							t.getFactory().getClass().getSimpleName() + ", " + t.fromBlueprint().name);
				}
			});

			mapEntities.forEach(t -> {
				try {
					t.getFactory().createRenderers(register, map, t);
				} catch (Exception e) {
					reporting.addException(e,
							t.getFactory().getClass().getSimpleName() + ", " + t.fromBlueprint().name);
				}
			});

			if (map.isAltMode()) {
				mapEntities.forEach(t -> {
					try {
						t.getFactory().createQualityIcon(register, map, t);
					} catch (Exception e) {
						reporting.addException(e,
								t.getFactory().getClass().getSimpleName() + ", " + t.fromBlueprint().name);
					}
				});
			}

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
			for (MapEntity mapEntity : mapEntities) {
				int entityNumber = mapEntity.fromBlueprint().entityNumber;
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

					Optional<WirePoint> firstPoint = first.getFactory().createWirePoint(register, first.getPosition(),
							orientation1, wire.firstWireConnectorId);
					Optional<WirePoint> secondPoint = second.getFactory().createWirePoint(register,
							second.getPosition(), orientation2, wire.secondWireConnectorId);

					if (!firstPoint.isPresent() || !secondPoint.isPresent()) {
						continue;// Probably something modded
					}

					register.accept(new MapWire(firstPoint.get().getPosition(), secondPoint.get().getPosition(),
							firstPoint.get().getColor().getColor()));
					register.accept(new MapWireShadow(firstPoint.get().getShadow(), secondPoint.get().getShadow()));
				} catch (Exception e) {
					reporting.addException(e, "Wire " + wire.firstEntityNumber + ", " + wire.firstWireConnectorId + ", "
							+ wire.secondEntityNumber + ", " + wire.secondWireConnectorId);
				}
			}

			if (map.isAltMode() && blueprint.snapToGrid.isPresent()) {
				BSPosition dim = blueprint.snapToGrid.get();
				register.accept(new MapSnapToGrid(MapRect.byUnit(0, 0, dim.x, dim.y)));
			}

			register.accept(new MapDebug(request.debug, map, mapEntities, mapTiles));

			register.accept(new MapItemLogistics(map));
			register.accept(new MapRailLogistics(map));
		}

		private void calculateBounds() {
			boolean showGrid = !request.getGridLines().isEmpty();
			boolean gridFoundationMode = map.isFoundation() && !request.show.gridNumbers;
			boolean gridShowNumbers = !gridFoundationMode && request.show.gridNumbers;
			boolean gridAboveBelts = request.show.gridAboveBelts;

			double gridPadding = (showGrid && gridShowNumbers) ? 1 : 0;
			double worldPadding = 0.1;

			MapRect3D gridBounds = calculateGridBounds(mapEntities, mapTiles, blueprint.snapToGrid);

			screenBounds = new Rectangle2D.Double();
			screenBounds.setFrameFromDiagonal(gridBounds.getX1() - worldPadding - gridPadding,
					gridBounds.getY1() - worldPadding - gridPadding, gridBounds.getX2() + worldPadding + gridPadding,
					gridBounds.getY2() + worldPadding + gridPadding);

			if (request.dontClipSprites()) {
				List<MapRect> rects = renderBuckets.values().stream().filter(r -> r instanceof MapBounded)
						.map(r -> ((MapBounded) r).getBounds()).collect(Collectors.toList());
				if (!rects.isEmpty()) {
					MapRect spriteBounds = MapRect.combineAll(rects);

					double x1 = spriteBounds.getX();
					double y1 = spriteBounds.getY();
					double x2 = x1 + spriteBounds.getWidth();
					double y2 = y1 + spriteBounds.getHeight();

					screenBounds.setFrameFromDiagonal(Math.min(screenBounds.getMinX(), x1),
							Math.min(screenBounds.getMinY(), y1), Math.max(screenBounds.getMaxX(), x2),
							Math.max(screenBounds.getMaxY(), y2));
				}
			}

			worldRenderScale = 1;

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

			boolean gridTooSmall = (1 / worldRenderScale) > 5;
			if (gridTooSmall) {
				showGrid = false;
			}

			if (showGrid) {
				if (gridFoundationMode) {
					register.accept(new MapFoundationGrid(mapTiles, request.getGridLines().get(), gridAboveBelts));
				} else {
					register.accept(
							new MapGrid(gridBounds, request.getGridLines().get(), gridAboveBelts, gridShowNumbers));
				}
			}

			imageWidth = Math.max(minWidthPixels,
					Math.min(maxWidthPixels, (int) Math.round(screenBounds.getWidth() * worldRenderScale * TILE_SIZE)));
			imageHeight = Math.max(minHeightPixels, Math.min(maxHeightPixels,
					(int) Math.round(screenBounds.getHeight() * worldRenderScale * TILE_SIZE)));
		}

		private void renderImage() {
			image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = image.createGraphics();

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

			// Background
			if (request.getBackground().isPresent()) {
				g.setColor(request.getBackground().get());
				g.fill(screenBounds);
			}

			for (Entry<Layer, List<MapRenderable>> entry : Multimaps.asMap(renderBuckets).entrySet()) {
				Layer layer = entry.getKey();
				List<MapRenderable> layerRenderers = entry.getValue();

				if (layer == Layer.SHADOW_BUFFER) {

					BufferedImage shadowImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
					Graphics2D shadowG = shadowImage.createGraphics();
					shadowG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					shadowG.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
							RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
					shadowG.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
							RenderingHints.VALUE_INTERPOLATION_BILINEAR);
					shadowG.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
					shadowG.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
							RenderingHints.VALUE_FRACTIONALMETRICS_ON);
					shadowG.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
					shadowG.setTransform(worldXform);

					for (MapRenderable renderer : layerRenderers) {
						try {
							renderer.render(shadowG);
						} catch (Exception e) {
							reporting.addException(e);
						}
					}

					shadowG.dispose();

					AffineTransform tempXform = g.getTransform();
					g.setTransform(noXform);
					Composite pc = g.getComposite();
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
					g.drawImage(shadowImage, 0, 0, null);
					g.setComposite(pc);
					g.setTransform(tempXform);

				} else {
					for (MapRenderable renderer : layerRenderers) {
						try {
							renderer.render(g);
						} catch (Exception e) {
							reporting.addException(e);
						}
					}
				}
			}
			g.dispose();
		}
	}

	public static FactorioManager getFactorioManager() {
		return factorioManager;
	}

	public static GUIStyle getGuiStyle() {
		return guiStyle;
	}

	public static IconManager getIconManager() {
		return iconManager;
	}

	private static void addToItemAmount(Map<BSItemWithQualityID, Double> items, BSItemWithQualityID item, double add) {
		double amount = items.getOrDefault(item, 0.0);
		amount += add;
		items.put(item, amount);
	}

	public static Map<BSItemWithQualityID, Double> generateTotalItems(BSBlueprint blueprint) {

		ModdingResolver resolver = ModdingResolver.byBlueprintBiases(factorioManager, blueprint);

		Map<BSItemWithQualityID, Double> ret = new LinkedHashMap<>();
		for (BSEntity entity : blueprint.entities) {
			EntityRendererFactory entityFactory = resolver.resolveFactoryEntityName(entity.name);
			if (entityFactory.isUnknown()) {
				addToItemAmount(ret, new BSItemWithQualityID(entity.name, entity.quality), 1);
				continue;
			}

			EntityPrototype entityPrototype = entityFactory.getPrototype();

			Optional<ItemToPlace> primaryItem = entityPrototype.getPrimaryItem();
			if (primaryItem.isEmpty()) {
				LOGGER.warn("MISSING ENTITY ITEM: {}", entity.name);
				continue;
			}

			addToItemAmount(ret, new BSItemWithQualityID(primaryItem.get().getItem(), entity.quality), primaryItem.get().getCount());

			for (BSItemStack itemStack : entity.items) {
				addToItemAmount(ret, itemStack.id, itemStack.getTotalCount());
			}
		}
		for (BSTile tile : blueprint.tiles) {
			String tileName = tile.name;
			TileRendererFactory tileFactory = resolver.resolveFactoryTileName(tileName);
			if (tileFactory.isUnknown()) {
				addToItemAmount(ret, new BSItemWithQualityID(tile.name, Optional.empty()), 1);
				continue;
			}

			TilePrototype tilePrototype = tileFactory.getPrototype();

			Optional<ItemToPlace> primaryItem = tilePrototype.getPrimaryItem();
			if (primaryItem.isEmpty()) {
				LOGGER.warn("MISSING TILE ITEM: {}", tilePrototype.getName());
				continue;
			}

			addToItemAmount(ret, new BSItemWithQualityID(primaryItem.get().getItem(), Optional.empty()), primaryItem.get().getCount());
		}
		return ret;
	}

	public static Map<BSItemWithQualityID, Double> generateTotalRawItems(Map<BSItemWithQualityID, Double> totalItems) {
		DataTable baseTable = factorioManager.getProfileVanilla().getFactorioData().getTable();
		Map<String, RecipePrototype> recipes = baseTable.getRecipes();
		Map<BSItemWithQualityID, Double> ret = new LinkedHashMap<>();
		TotalRawCalculator calculator = new TotalRawCalculator(recipes);
		for (Entry<BSItemWithQualityID, Double> entry : totalItems.entrySet()) {
			BSItemWithQualityID recipeItem = entry.getKey();
			double recipeAmount = entry.getValue();
			baseTable.getRecipe(recipeItem.name).ifPresent(r -> {
				Map<String, Double> outputs = r.getOutputs();
				if (outputs.containsKey(recipeItem.name)) {
					double multiplier = recipeAmount / outputs.get(recipeItem.name);
					Map<String, Double> totalRaw = calculator.compute(r);
					for (Entry<String, Double> entry2 : totalRaw.entrySet()) {
						String itemName = entry2.getKey();
						double itemAmount = entry2.getValue();
						addToItemAmount(ret, new BSItemWithQualityID(itemName, Optional.empty()), itemAmount * multiplier);
					}
				}
			});
		}
		return ret;
	}

	public static synchronized boolean initialize() {
		if (initialized) {
			return true;
		}
		initialized = true;

		//Ignoring profiles that are not ready
		List<Profile> allProfiles = Profile.listProfiles();
		List<Profile> profiles = new ArrayList<>(allProfiles.stream().filter(p -> p.getStatus() == ProfileStatus.READY).collect(Collectors.toList()));
		LOGGER.info("READY PROFILES: {}", profiles.stream().map(Profile::getName).collect(Collectors.joining(", ")));

		if (profiles.size() != allProfiles.size()) {
			LOGGER.warn("NOT READY PROFILES: {}", 
				allProfiles.stream().filter(p -> p.getStatus() != ProfileStatus.READY).map(Profile::getName).collect(Collectors.joining(", ")));
		}

		if (profiles.isEmpty()) {
			System.out.println("No ready profiles found! Please ensure at least one profile is ready.");
			return false;
		}

		factorioManager = new FactorioManager(profiles);
		guiStyle = new GUIStyle();
		iconManager = new IconManager(factorioManager);

		for (Profile profile : profiles) {
			profile.setFactorioManager(factorioManager);
			profile.setGuiStyle(guiStyle);
			profile.setIconManager(iconManager);
		}

		if (!factorioManager.initializePrototypes()) {
			System.out.println("Failed to initialize prototypes.");
			return false;
		}

		guiStyle.initialize(factorioManager.getProfileVanilla());

		if (!factorioManager.initializeFactories()) {
			System.out.println("Failed to initialize factories.");
			return false;
		}

		iconManager.initialize();

		for (Profile profile : factorioManager.getProfiles()) {
			if (!profile.getAtlasPackage().initialize()) {
				System.out.println("Failed to initialize atlas package for profile: " + profile.getName());
				return false;
			}
		}

		return true;
	}

	public static boolean buildData(Profile profile) {

		profile.resetLoadedData();

		if (!FactorioManager.hasFactorioInstall()) {
			System.out.println("No Factorio install found, cannot build data for profile: " + profile.getName());
			return false;
		}

		List<Profile> profiles = new ArrayList<>();
		profiles.add(profile);
		
		Profile profileVanilla;
		if (!profile.isVanilla()) {
			profileVanilla = Profile.vanilla();

			if (!profileVanilla.hasDump()) {
				System.out.println("Vanilla profile must be built first, cannot build data for profile: " + profile.getName());
				return false;
			}

			profileVanilla.resetLoadedData();

			profiles.add(profileVanilla);
		
		} else {
			profileVanilla = profile;
		}

		FactorioManager factorioManager = new FactorioManager(profiles);
		GUIStyle guiStyle = new GUIStyle();
		IconManager iconManager = new IconManager(factorioManager);

		for (Profile p : profiles) {
			p.setFactorioManager(factorioManager);
			p.setGuiStyle(guiStyle);
			p.setIconManager(iconManager);
		}

		if (profile.isVanilla()) {
			if (!GUIStyle.copyFontsToProfile(profile)) {
                System.out.println("Failed to copy fonts to vanilla profile.");
                return false;
            }
		}
		
		try {
			if (!factorioManager.initializePrototypes()) {
				return false;
			}
			guiStyle.initialize(profileVanilla);
			if (!factorioManager.initializeFactories()) {
				return false;
			}
			iconManager.initialize();

			profile.getAtlasPackage().generateZip();

		} catch (JSONException | IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	private static void populateRailBlocking(WorldMap map, boolean elevated) {
		// TODO fix rail logistics, redesign nodes as a virtual structure of the rails

//		map.getRailNodes(elevated).cellSet().stream().filter(c -> c.getValue().hasSignals()).forEach(c -> {
//			RailNode blockingNode = c.getValue();
//			Set<RailDirection> signals = blockingNode.getSignals();
//			for (RailDirection signalDir : signals) {
//				RailDirection blockingDir = signalDir.back();
//				if (signals.contains(blockingDir)) {
//					continue;
//				}
//
//
//				{
//					Queue<RailEdge> work = new ArrayDeque<>();
//					work.addAll(blockingNode.getOutgoingEdges(blockingDir));
//					while (!work.isEmpty()) {
//						RailEdge edge = work.poll();
//						if (edge.isBlocked()) {
//							continue;
//						}
//						edge.setBlocked(true);
//						RailNode node = map.getRailNode(edge.getEnd().pos, elevated).get();
//						if (node.hasSignals()) {
//							continue;
//						}
//						if (node.getIncomingEdges(edge.getEnd().dir).stream().allMatch(e -> e.isBlocked())) {
//							work.addAll(node.getOutgoingEdges(edge.getEnd().dir.back()));
//						}
//					}
//				}
//
//				{
//					Queue<RailEdge> work = new ArrayDeque<>();
//					work.addAll(blockingNode.getIncomingEdges(blockingDir.back()));
//					while (!work.isEmpty()) {
//						RailEdge edge = work.poll();
//						if (edge.isBlocked()) {
//							continue;
//						}
//						edge.setBlocked(true);
//						RailNode node = map.getRailNode(edge.getStart().pos, elevated).get();
//						if (node.hasSignals()) {
//							continue;
//						}
//						if (node.getOutgoingEdges(edge.getStart().dir).stream().allMatch(e -> e.isBlocked())) {
//							work.addAll(node.getIncomingEdges(edge.getStart().dir.back()));
//						}
//					}
//				}
//
//				// for (RailEdge startEdge :
//				// blockingNode.getOutgoingEdges(blockingDir)) {
//				// startEdge.setBlocked(true);
//				// RailNode node = map.getRailNode(startEdge.getEndPos()).get();
//				// Direction dir = startEdge.getEndDir();
//				// Collection<RailEdge> edges;
//				// while (!node.hasSignals() && ((edges =
//				// node.getOutgoingEdges(dir)).size() == 1)) {
//				// RailEdge edge = edges.iterator().next();
//				// if (edge.isBlocked()) {
//				// break;
//				// }
//				// edge.setBlocked(true);
//				// node = map.getRailNode(edge.getEndPos()).get();
//				// dir = edge.getEndDir();
//				// }
//				// }
//				//
//				// for (RailEdge startEdge :
//				// blockingNode.getIncomingEdges(blockingDir)) {
//				// startEdge.setBlocked(true);
//				// RailNode node = map.getRailNode(startEdge.getEndPos()).get();
//				// Direction dir = startEdge.getEndDir();
//				// Collection<RailEdge> edges;
//				// while (!node.hasSignals() && ((edges =
//				// node.getIncomingEdges(dir)).size() == 1)) {
//				// RailEdge edge = edges.iterator().next();
//				// if (edge.isBlocked()) {
//				// break;
//				// }
//				// edge.setBlocked(true);
//				// node = map.getRailNode(edge.getEndPos()).get();
//				// dir = edge.getEndDir();
//				// }
//				// }
//			}
//		});
	}

	private static void populateRailStationLogistics(WorldMap map) {
		// TODO fix rail logistics, redesign nodes as a virtual structure of the rails

//		map.getRailNodes(false).cellSet().stream().filter(c -> c.getValue().getStation().isPresent()).forEach(c -> {
//			RailNode stationNode = c.getValue();
//			RailDirection stationDir = stationNode.getStation().get();
//
//			{
//				Queue<RailEdge> work = new ArrayDeque<>();
//				work.addAll(stationNode.getOutgoingEdges(stationDir));
//				work.addAll(stationNode.getOutgoingEdges(stationDir.back()));
//				while (!work.isEmpty()) {
//					RailEdge edge = work.poll();
//					if (edge.isBlocked() || edge.isOutput()) {
//						continue;
//					}
//					edge.setOutput(true);
//					RailNode node = map.getRailNode(edge.getEnd().pos, false).get();
//					if (node.getIncomingEdges(edge.getEnd().dir).stream().allMatch(e -> e.isOutput())) {
//						work.addAll(node.getOutgoingEdges(edge.getEnd().dir.back()));
//					}
//				}
//			}
//
//			{
//				Queue<RailEdge> work = new ArrayDeque<>();
//				work.addAll(stationNode.getIncomingEdges(stationDir.back()));
//				while (!work.isEmpty()) {
//					RailEdge edge = work.poll();
//					if (edge.isBlocked() || edge.isInput()) {
//						continue;
//					}
//					edge.setInput(true);
//					RailNode node = map.getRailNode(edge.getStart().pos, false).get();
//					if (node.getOutgoingEdges(edge.getStart().dir).stream().allMatch(e -> e.isInput())) {
//						work.addAll(node.getIncomingEdges(edge.getStart().dir.back()));
//					}
//				}
//			}
//		});
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

	public static RenderResult renderBlueprint(RenderRequest request) {
		return new ImageRenderer(request).call();
	}

	public static Future<RenderResult> renderBlueprintAsync(RenderRequest request) {
		return executor.submit(new ImageRenderer(request));
	}

	private static MapRect3D calculateGridBounds(List<MapEntity> mapEntities, List<MapTile> mapTiles, Optional<BSPosition> snapToGrid) {

		int tilefp = MapUtils.unitToFixedPoint(1.0);

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
			int wfp = bounds.getWidthFP() + tilefp;
			int hfp = bounds.getHeightFP() + tilefp;
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

		if (snapToGrid.isPresent()) {
			MapPosition dim = snapToGrid.get().createPoint();
			x1fp = Math.min(x1fp, 0);
			y1fp = Math.min(y1fp, 0);
			x2fp = Math.max(x2fp, dim.getXFP());
			y2fp = Math.max(y2fp, dim.getYFP());
		}

		return MapRect3D.byFixedPoint(x1fp, y1fp, x2fp, y2fp, heightfp);
	}

	public static class RenderDebugLayersResult {
		public final BufferedImage image;
		public final List<MapRenderable> renderables;

		public RenderDebugLayersResult(BufferedImage image, List<MapRenderable> renderables) {
			this.image = image;
			this.renderables = renderables;
		}
	}

	public static RenderDebugLayersResult renderDebugLayers(EntityRendererFactory factory, JSONObject jsonEntity, ModdingResolver resolver)
			throws Exception {

		ListMultimap<Layer, MapRenderable> renderOrder = MultimapBuilder.enumKeys(Layer.class).arrayListValues()
				.build();
		Consumer<MapRenderable> register = r -> renderOrder.put(r.getLayer(), r);

		BSEntity bsEntity = factory.parseEntity(jsonEntity);
		MapEntity entity = new MapEntity(bsEntity, factory, resolver);

		WorldMap map = new WorldMap();
		map.setAltMode(true);

		factory.populateWorldMap(map, entity);
		factory.populateLogistics(map, entity);
		factory.initWireConnector(register, entity, ImmutableList.of());
		factory.createRenderers(register, map, entity);

		List<MapRenderable> renderables = renderOrder.values().stream().collect(Collectors.toList());
		Collections.reverse(renderables);

		List<MapRect> rects = renderables.stream().filter(r -> r instanceof MapBounded)
				.map(r -> ((MapBounded) r).getBounds()).collect(Collectors.toList());
		MapRect frameBounds = MapRect.combineAll(rects);
		frameBounds = frameBounds.expandUnit(1);
		Rectangle frame = frameBounds.toPixels();

		int rows = (int) Math.ceil(Math.sqrt(renderables.size()));
		int cols = (renderables.size() + rows - 1) / rows;

		BufferedImage image = new BufferedImage(frame.width * cols, frame.height * rows, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

		g.setColor(Color.darkGray);
		g.fillRect(0, 0, image.getWidth(), image.getHeight());

		g.setColor(Color.black);
		for (int i = 0; i < renderables.size(); i++) {
			int x = (i % cols) * frame.width;
			int y = (i / cols) * frame.height;
			g.drawRect(x + 64 - 1, y + 64 - 1, frame.width - 64 * 2 + 1, frame.height - 64 * 2 + 1);
		}

		g.scale(image.getWidth() / (frameBounds.getWidth() * cols),
				image.getHeight() / (frameBounds.getHeight() * rows));
		g.translate(-frameBounds.getX(), -frameBounds.getY());
		AffineTransform pt = g.getTransform();

		MapText label = new MapText(null,
				MapPosition.byUnit(frameBounds.getX() + 0.15, frameBounds.getY() + frameBounds.getHeight() / 2.0), 0,
				factory.getProfile().getGuiStyle().FONT_BP_BOLD.deriveFont(0.8f), Color.white, "", false);

		int i = 0;
		for (MapRenderable renderable : renderables) {
			g.setTransform(pt);
			g.translate(frameBounds.getWidth() * (i % cols), frameBounds.getHeight() * (i / cols));

			renderable.render(g);

			label.setString("" + (++i));
			label.render(g);

			if (renderable instanceof MapBounded) {
				Stroke ps = g.getStroke();
				g.setStroke(new BasicStroke(1 / 64f));
				g.setColor(Color.black);
				MapRect b = ((MapBounded) renderable).getBounds();
				g.draw(new Rectangle2D.Double(b.getX() - 1 / 64f, b.getY() - 1 / 64f, b.getWidth() + 1 / 64f,
						b.getHeight() + 1 / 64f));
				g.setStroke(ps);
			}
		}

		g.dispose();

		return new RenderDebugLayersResult(image, renderables);
	}
}
