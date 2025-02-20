package com.demod.fbsr;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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

public abstract class EntityRendererFactory<E extends BSEntity> {

	private static final Logger LOGGER = LoggerFactory.getLogger(EntityRendererFactory.class);

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void initFactories(List<EntityRendererFactory> factories) {
		for (EntityRendererFactory factory : factories) {
			EntityPrototype prototype = factory.getPrototype();
			try {
				factory.initFromPrototype();
				factory.wirePointsById = new LinkedHashMap<>();
				factory.defineWirePoints(factory.wirePointsById::put, prototype.lua());
				factory.drawBounds = factory.computeBounds();
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

	@SuppressWarnings({ "rawtypes" })
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
	protected FPBoundingBox protoSelectionBox;
	protected boolean protoBeaconed;

	protected MapRect3D drawBounds;

	protected Map<Integer, WirePoints> wirePointsById;

	private Class<E> entityClass;

	protected void addLogisticWarp(WorldMap map, MapPosition gridPos1, Direction cellDir1, MapPosition gridPos2,
			Direction cellDir2) {
		map.getOrCreateLogisticGridCell(cellDir1.offset(gridPos1, 0.25)).addWarp(cellDir2.offset(gridPos2, 0.25));
	}

	public void createModuleIcons(Consumer<MapRenderable> register, WorldMap map, MapRect3D bounds, E entity) {

		Multiset<String> renderModules = RenderUtils.getModules(entity);
		if (!renderModules.isEmpty()) {

			double x = entity.position.x - 0.7 * (renderModules.size() / 2.0) + 0.35;
			double y = entity.position.y + 0.7;

			for (String itemName : renderModules) {
				BufferedImage image = FactorioManager.lookupItemByName(itemName)
						.map(i -> i.getTable().getData().getWikiIcon(i)).orElse(RenderUtils.EMPTY_IMAGE);
				register.accept(new MapIcon(MapPosition.byUnit(x, y), image, 0.5, 0.05));
				x += 0.7;
			}
		}

		if (protoBeaconed) {
			MapPosition pos = entity.position.createPoint();
			Rectangle2D.Double beaconedBounds = protoSelectionBox.createRect();
			beaconedBounds.x += pos.getX();
			beaconedBounds.y += pos.getY();
			Set<BeaconSource> beacons = new LinkedHashSet<>();
			map.getBeaconed(new Point2D.Double(beaconedBounds.x + 0.5, beaconedBounds.y + 0.5))
					.ifPresent(beacons::addAll);
			map.getBeaconed(new Point2D.Double(beaconedBounds.x + 0.5, beaconedBounds.y + beaconedBounds.height - 0.5))
					.ifPresent(beacons::addAll);
			map.getBeaconed(new Point2D.Double(beaconedBounds.x + beaconedBounds.width - 0.5,
					beaconedBounds.y + beaconedBounds.height - 0.5)).ifPresent(beacons::addAll);
			map.getBeaconed(new Point2D.Double(beaconedBounds.x + beaconedBounds.width - 0.5, beaconedBounds.y + 0.5))
					.ifPresent(beacons::addAll);

			if (!beacons.isEmpty()) {
				Map<String, Double> beaconModules = new LinkedHashMap<>();
				for (BeaconSource bs : beacons) {
					BSEntity beacon = bs.getBeacon();
					double distributionEffectivity = bs.getDistributionEffectivity();

					Multiset<String> modules2 = RenderUtils.getModules(beacon);
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

				double x = entity.position.x - 0.3 * (renderModules.size() / 2.0) + 0.15;
				double y = entity.position.y - 1.15;

				for (String itemName : renderModules) {
					BufferedImage image = FactorioManager.lookupItemByName(itemName)
							.map(i -> i.getTable().getData().getWikiIcon(i)).orElse(RenderUtils.EMPTY_IMAGE);
					register.accept(new MapIcon(MapPosition.byUnit(x, y), image, 0.25, 0.025));
					x += 0.3;
				}
			}
		}
	}

	public abstract void createRenderers(Consumer<MapRenderable> register, WorldMap map, E entity);

	public Optional<WirePoint> createWirePoint(Consumer<MapRenderable> register, MapPosition position,
			double orientation, int connectionId) {
		return Optional.ofNullable(wirePointsById.get(connectionId)).map(wp -> wp.getPoint(position, orientation));
	}

	public void defineWirePoints(BiConsumer<Integer, WirePoints> consumer, LuaTable lua) {

	}

	public FPBoundingBox getSelectionBox() {
		return protoSelectionBox;
	}

	public FactorioData getData() {
		return data;
	}

	public MapRect3D getDrawBounds(E entity) {
		return entity.direction.rotate(drawBounds).shift(entity.position.x, entity.position.y);
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

	@SuppressWarnings("unchecked")
	private void initEntityClass() {
		// Looks for the subclass that specifies the E class explicitly

		if (entityClass == null) {
			Class<?> clazz = this.getClass();
			Type type = null;

			while (clazz != null && clazz != Object.class) {
				Type superclass = clazz.getGenericSuperclass();

				if (superclass instanceof ParameterizedType) {
					ParameterizedType parameterizedType = (ParameterizedType) superclass;
					Type rawType = parameterizedType.getRawType();

					if (rawType instanceof Class<?>
							&& EntityRendererFactory.class.isAssignableFrom((Class<?>) rawType)) {
						type = parameterizedType.getActualTypeArguments()[0];
						break;
					}
				}
				clazz = clazz.getSuperclass();
			}

			if (type instanceof Class<?>) {
				entityClass = (Class<E>) type;
			} else if (type instanceof ParameterizedType) {
				entityClass = (Class<E>) ((ParameterizedType) type).getRawType();
			} else {
				throw new RuntimeException("Unable to determine entity class for " + this.getClass().getSimpleName());
			}
		}
	}

	public abstract void initFromPrototype();

	// Returns orientation if applicable
	public double initWireConnector(Consumer<MapRenderable> register, E entity, List<MapEntity> wired) {
		return 0;
	}

	public boolean isUnknown() {
		return false;
	}

	public E parseEntity(JSONObject json) throws Exception {
		initEntityClass();
		Constructor<E> constructor = entityClass.getConstructor(JSONObject.class);
		return constructor.newInstance(json);
	}

	public E parseEntity(LegacyBlueprintEntity legacy) throws Exception {
		initEntityClass();
		Constructor<E> constructor = entityClass.getConstructor(LegacyBlueprintEntity.class);
		return constructor.newInstance(legacy);
	}

	public void populateLogistics(WorldMap map, E entity) {
		// default do nothing
	}

	public void populateWorldMap(WorldMap map, E entity) {
		// default do nothing
	}

	public void setData(FactorioData data) {
		this.data = data;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	protected void setLogisticAcceptFilter(WorldMap map, Point2D.Double gridPos, Direction cellDir,
			Direction acceptFilter) {
		LogisticGridCell cell = map.getOrCreateLogisticGridCell(cellDir.offset(gridPos, 0.25));
		cell.setAcceptFilter(Optional.of(acceptFilter));
	}

	protected void setLogisticMachine(WorldMap map, BSEntity entity, RecipePrototype recipe) {
		MapPosition entityPos = entity.position.createPoint();
		Rectangle2D.Double box = protoSelectionBox.createRect();
		double xStart = entityPos.getX() + box.x;
		double yStart = entityPos.getY() + box.y;
		double xEnd = xStart + box.width;
		double yEnd = yStart + box.height;

		DataTable table = data.getTable();

		Set<String> inputs = recipe.getInputs().keySet().stream().filter(k -> table.getItem(k).isPresent())
				.collect(Collectors.toSet());
		Set<String> outputs = recipe.getOutputs().keySet().stream().filter(k -> table.getItem(k).isPresent())
				.collect(Collectors.toSet());

		Point2D.Double cellPos = new Point2D.Double();
		for (cellPos.x = xStart + 0.25; cellPos.x < xEnd; cellPos.x += 0.5) {
			for (cellPos.y = yStart + 0.25; cellPos.y < yEnd; cellPos.y += 0.5) {
				LogisticGridCell cell = map.getOrCreateLogisticGridCell(cellPos);
				cell.setInputs(Optional.of(inputs));
				cell.setOutputs(Optional.of(outputs));
				cell.setBlockTransit(true);
			}
		}
	}

	protected void setLogisticMove(WorldMap map, Point2D.Double gridPos, Direction cellDir, Direction moveDir) {
		map.getOrCreateLogisticGridCell(cellDir.offset(gridPos, 0.25)).setMove(Optional.of(moveDir));
	}

	protected void setLogisticMoveAndAcceptFilter(WorldMap map, Point2D.Double gridPos, Direction cellDir,
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

		// FIXME ideally there shouldn't be a need for selection box and beaconed check

		// TODO should move FPBoundingBox to data wrapper repo
		Rectangle2D.Double sb = prototype.getSelectionBox();
		protoSelectionBox = new FPBoundingBox(sb.getMinX(), sb.getMinY(), sb.getMaxX(), sb.getMaxY());

		// FIXME Hard-coding
		protoBeaconed = (!prototype.lua().get("module_specification").isnil()
				|| prototype.getName().equals("assembling-machine-1")
				|| prototype.getName().equals("burner-mining-drill")) && !prototype.getName().equals("beacon");
	}

}
