package com.demod.fbsr;

import java.awt.geom.Point2D;
import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.factorio.prototype.RecipePrototype;
import com.demod.fbsr.WirePoints.WirePoint;
import com.demod.fbsr.WorldMap.BeaconSource;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.fp.FPBoundingBox;
import com.demod.fbsr.fp.FPVector;
import com.demod.fbsr.legacy.LegacyBlueprintEntity;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapIcon;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRect3D;
import com.demod.fbsr.map.MapRenderable;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;

public abstract class EntityRendererFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(EntityRendererFactory.class);

	public static void initFactories(List<EntityRendererFactory> factories) {
		for (EntityRendererFactory factory : factories) {
			EntityPrototype prototype = factory.getPrototype();
			try {
				factory.initFromPrototype();
				factory.wirePointsById = new LinkedHashMap<>();
				factory.defineWirePoints(factory.wirePointsById::put, prototype.lua());
				factory.drawBounds = factory.computeBounds();
				factory.initAtlas(AtlasManager::registerDef);
			} catch (Exception e) {
				LOGGER.error("ENTITY {} ({})", prototype.getName(), prototype.getType());
				throw e;
			}
		}

		LOGGER.info("Initialized {} entities.", factories.size());
	}

	protected MapRect3D computeBounds() {
		LuaTable lua = prototype.lua();
		Optional<FPBoundingBox> selectionBox = FPUtils.opt(lua.get("selection_box"), FPBoundingBox::new);
		Optional<FPBoundingBox> collisionBox = FPUtils.opt(lua.get("collision_box"), FPBoundingBox::new);
		double drawingBoxVerticalExtension = lua.get("drawing_box_vertical_extension").optdouble(0);

		// Combine selection and collision boxes, add vertical extension
		FPVector sb1;
		FPVector sb2;
		if (selectionBox.isPresent()) {
			sb1 = selectionBox.get().leftTop;
			sb2 = selectionBox.get().rightBottom;
		} else {
			sb1 = new FPVector(0, 0);
			sb2 = new FPVector(0, 0);
		}
		FPVector cb1;
		FPVector cb2;
		if (collisionBox.isPresent()) {
			cb1 = collisionBox.get().leftTop;
			cb2 = collisionBox.get().rightBottom;
		} else {
			cb1 = new FPVector(0, 0);
			cb2 = new FPVector(0, 0);
		}
		double x1 = Math.min(sb1.x, cb1.x);
		double y1 = Math.min(sb1.y, cb1.y);
		double x2 = Math.max(sb2.x, cb2.x);
		double y2 = Math.max(sb2.y, cb2.y);

		return MapRect3D.byUnit(x1, y1, x2, y2, drawingBoxVerticalExtension);
	}

	public static void registerFactories(Consumer<EntityRendererFactory> register, FactorioData data, JSONObject json) {
		DataTable table = data.getTable();
		for (String groupName : json.keySet().stream().sorted().collect(Collectors.toList())) {
			JSONObject jsonGroup = json.getJSONObject(groupName);
			for (String entityName : jsonGroup.keySet().stream().sorted().collect(Collectors.toList())) {
				Optional<EntityPrototype> entity = table.getEntity(entityName);
				if (!entity.isPresent()) {
					LOGGER.warn("MISSING ENTITY: {}", entityName);
					continue;
				}
				EntityPrototype prototype = entity.get();
				String factoryName = jsonGroup.getString(entityName);
				String factoryClassName = "com.demod.fbsr.entity." + factoryName;
				try {
					EntityRendererFactory factory = (EntityRendererFactory) Class.forName(factoryClassName)
							.getConstructor().newInstance();
					factory.setName(entityName);
					factory.setGroupName(groupName);
					factory.setData(data);
					factory.setPrototype(prototype);
					register.accept(factory);
				} catch (Exception e) {
					prototype.debugPrint();
					LOGGER.error("FACTORY CLASS: {}", factoryClassName, e);
					System.exit(-1);
				}
			}
		}
	}

	protected String name = null;
	protected String groupName = null;
	protected FactorioData data = null;
	protected EntityPrototype prototype = null;

	protected boolean protoBeaconed;
	protected MapRect3D drawBounds;
	protected Map<Integer, WirePoints> wirePointsById;

	private Class<? extends BSEntity> entityClass;
	private Constructor<? extends BSEntity> entityConstructorByJSON;
	private Constructor<? extends BSEntity> entityConstructorByLegacy;

	protected void addLogisticWarp(WorldMap map, MapPosition gridPos1, Direction cellDir1, MapPosition gridPos2,
			Direction cellDir2) {
		map.getOrCreateLogisticGridCell(cellDir1.offset(gridPos1, 0.25)).addWarp(cellDir2.offset(gridPos2, 0.25));
	}

	public void createModuleIcons(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {

		MapPosition position = entity.getPosition();
		MapRect3D bounds = entity.getBounds();

		Multiset<String> renderModules = entity.getModules();
		if (!renderModules.isEmpty()) {

			double x = position.getX() - 0.7 * (renderModules.size() / 2.0) + 0.35;
			double y = position.getY() + 0.7;

			for (String itemName : renderModules) {
				Optional<ImageDef> image = TagManager.lookup("item", itemName);
				if (image.isPresent()) {
					register.accept(new MapIcon(MapPosition.byUnit(x, y), image.get(), 0.5, 0.05, true));
					x += 0.7;
				}
			}
		}

		if (protoBeaconed) {
			Set<BeaconSource> beacons = new LinkedHashSet<>();
			map.getBeaconed(Direction.SOUTHEAST.offset(bounds.getTopLeft(), 0.5)).ifPresent(beacons::addAll);
			map.getBeaconed(Direction.SOUTHWEST.offset(bounds.getTopRight(), 0.5)).ifPresent(beacons::addAll);
			map.getBeaconed(Direction.NORTHEAST.offset(bounds.getBottomLeft(), 0.5)).ifPresent(beacons::addAll);
			map.getBeaconed(Direction.NORTHWEST.offset(bounds.getBottomRight(), 0.5)).ifPresent(beacons::addAll);

			if (!beacons.isEmpty()) {
				Map<String, Double> beaconModules = new LinkedHashMap<>();
				for (BeaconSource bs : beacons) {
					MapEntity beacon = bs.getBeacon();
					double distributionEffectivity = bs.getDistributionEffectivity();

					Multiset<String> modules2 = beacon.getModules();
					for (Multiset.Entry<String> entry : modules2.entrySet()) {
						double amount = beaconModules.getOrDefault(entry.getElement(), 0.0);
						amount += distributionEffectivity * entry.getCount();
						beaconModules.put(entry.getElement(), amount);
					}
				}

				Multiset<String> modules = LinkedHashMultiset.create(beaconModules.size());
				for (Map.Entry<String, Double> entry : beaconModules.entrySet()) {
					if (entry.getValue() >= 1) {
						modules.add(entry.getKey(), (int) (entry.getValue() + 0.01));
					}
				}

				double x = position.getX() - 0.3 * (renderModules.size() / 2.0) + 0.15;
				double y = position.getY() - 1.15;

				for (String itemName : renderModules) {
					Optional<ImageDef> image = TagManager.lookup("item", itemName);
					if (image.isPresent()) {
						register.accept(new MapIcon(MapPosition.byUnit(x, y), image.get(), 0.25, 0.025, true));
						x += 0.3;
					}
				}
			}
		}
	}

	public abstract void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity);

	public Optional<WirePoint> createWirePoint(Consumer<MapRenderable> register, MapPosition position,
			double orientation, int connectionId) {
		return Optional.ofNullable(wirePointsById.get(connectionId)).map(wp -> wp.getPoint(position, orientation));
	}

	public void defineWirePoints(BiConsumer<Integer, WirePoints> consumer, LuaTable lua) {

	}

	public FactorioData getData() {
		return data;
	}

	public MapRect3D getDrawBounds(MapEntity entity) {
		return entity.getDirection().rotate(drawBounds).shift(entity.getPosition());
	}

	public String getGroupName() {
		return groupName;
	}

	public String getName() {
		return name;
	}

	public EntityPrototype getPrototype() {
		return prototype;
	}

	private void initEntityClass() {
		entityClass = getEntityClass();
		try {
			entityConstructorByJSON = entityClass.getConstructor(JSONObject.class);
			entityConstructorByLegacy = entityClass.getConstructor(LegacyBlueprintEntity.class);
		} catch (NoSuchMethodException | SecurityException e) {
			LOGGER.error("ENTITY CLASS: {}", entityClass.getSimpleName());
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public abstract void initFromPrototype();

	public abstract void initAtlas(Consumer<ImageDef> register);

	// Returns orientation if applicable
	public double initWireConnector(Consumer<MapRenderable> register, MapEntity entity, List<MapEntity> wired) {
		return 0;
	}

	public boolean isUnknown() {
		return false;
	}

	public Class<? extends BSEntity> getEntityClass() {
		return BSEntity.class;
	}

	public BSEntity parseEntity(JSONObject json) throws Exception {
		return entityConstructorByJSON.newInstance(json);
	}

	public BSEntity parseEntityLegacy(LegacyBlueprintEntity legacy) throws Exception {
		return entityConstructorByLegacy.newInstance(legacy);
	}

	public void populateLogistics(WorldMap map, MapEntity entity) {
		// default do nothing
	}

	public void populateWorldMap(WorldMap map, MapEntity entity) {
		// default do nothing
	}

	public void setData(FactorioData data) {
		this.data = data;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	protected void setLogisticAcceptFilter(WorldMap map, MapPosition gridPos, Direction cellDir,
			Direction acceptFilter) {
		LogisticGridCell cell = map.getOrCreateLogisticGridCell(cellDir.offset(gridPos, 0.25));
		cell.setAcceptFilter(Optional.of(acceptFilter));
	}

	// TODO convert to fixed point maths
	protected void setLogisticMachine(WorldMap map, MapRect3D bounds, RecipePrototype recipe) {
		double xStart = bounds.getX1();
		double yStart = bounds.getY1();
		double xEnd = bounds.getX2();
		double yEnd = bounds.getY2();

		DataTable table = data.getTable();

		Set<String> inputs = recipe.getInputs().keySet().stream().filter(k -> table.getItem(k).isPresent())
				.collect(Collectors.toSet());
		Set<String> outputs = recipe.getOutputs().keySet().stream().filter(k -> table.getItem(k).isPresent())
				.collect(Collectors.toSet());

		Point2D.Double cellPos = new Point2D.Double();
		for (cellPos.x = xStart + 0.25; cellPos.x < xEnd; cellPos.x += 0.5) {
			for (cellPos.y = yStart + 0.25; cellPos.y < yEnd; cellPos.y += 0.5) {
				LogisticGridCell cell = map.getOrCreateLogisticGridCell(MapPosition.byUnit(cellPos.x, cellPos.y));
				cell.setInputs(Optional.of(inputs));
				cell.setOutputs(Optional.of(outputs));
				cell.setBlockTransit(true);
			}
		}
	}

	protected void setLogisticMove(WorldMap map, MapPosition gridPos, Direction cellDir, Direction moveDir) {
		map.getOrCreateLogisticGridCell(cellDir.offset(gridPos, 0.25)).setMove(Optional.of(moveDir));
	}

	protected void setLogisticMoveAndAcceptFilter(WorldMap map, MapPosition gridPos, Direction cellDir,
			Direction moveDir, Direction acceptFilter) {
		LogisticGridCell cell = map.getOrCreateLogisticGridCell(cellDir.offset(gridPos, 0.25));
		cell.setMove(Optional.of(moveDir));
		cell.setAcceptFilter(Optional.of(acceptFilter));
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPrototype(EntityPrototype prototype) {
		this.prototype = prototype;

		initEntityClass();

		// FIXME Hard-coding
		protoBeaconed = (!prototype.lua().get("module_specification").isnil()
				|| prototype.getName().equals("assembling-machine-1")
				|| prototype.getName().equals("burner-mining-drill")) && !prototype.getName().equals("beacon");
	}

}
