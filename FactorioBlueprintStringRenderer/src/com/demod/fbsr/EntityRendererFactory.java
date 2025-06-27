package com.demod.fbsr;

import java.awt.geom.Point2D;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.Map.Entry;
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
import com.demod.fbsr.Profile.ProfileModGroupRenderings;
import com.demod.fbsr.WirePoints.WirePoint;
import com.demod.fbsr.WorldMap.BeaconSource;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.def.IconDef;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.fp.FPBoundingBox;
import com.demod.fbsr.fp.FPVector;
import com.demod.fbsr.legacy.LegacyBlueprintEntity;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapEntity.EntityModule;
import com.demod.fbsr.map.MapIcon;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRect3D;
import com.demod.fbsr.map.MapRenderable;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;

public abstract class EntityRendererFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(EntityRendererFactory.class);

	protected MapRect3D computeBounds() {
		return defaultComputeBounds();
	}

	protected MapRect3D defaultComputeBounds() {
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

	public static boolean initFactories(Consumer<EntityRendererFactory> register, Profile profile) {
		DataTable table = profile.getFactorioData().getTable();
		boolean hasEntityTypeMismatch = false;
		for (ProfileModGroupRenderings renderings : profile.listRenderings()) {
			for (Entry<String, String> entry : renderings.getEntityMappings().entrySet()) {
				String entityName = entry.getKey();
				String factoryName = entry.getValue();

				Optional<EntityPrototype> optProto = table.getEntity(entityName);
				if (!optProto.isPresent()) {
					System.out.println("Rendering entity not found in factorio data: " + entityName);
					return false;
				}

				EntityPrototype proto = optProto.get();
				String factoryClassName = "com.demod.fbsr.entity." + factoryName;
				try {
					EntityRendererFactory factory = (EntityRendererFactory) Class.forName(factoryClassName)
							.getConstructor().newInstance();

					if (!factory.isEntityTypeMatch(proto)) {
						System.out.println("ENTITY MISMATCH " + entityName + " (" + factoryName + " ==> " + proto.getType() + ")");
						hasEntityTypeMismatch = true;
					}

					factory.setName(entityName);
					factory.setGroupName(renderings.getModGroup());
					factory.setProfile(profile);
					factory.setPrototype(proto);
					factory.initFromPrototype();
					factory.wirePointsById = new LinkedHashMap<>();
					factory.defineWirePoints(factory.wirePointsById::put, proto.lua());
					factory.drawBounds = factory.computeBounds();
					factory.initAtlas(factory.getProfile().getAtlasPackage()::registerDef);
					
					register.accept(factory);

				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("Problem registering rendering for entity: " + entityName);
					return false;
				}
			}
		}

		if (hasEntityTypeMismatch) {
			System.out.println("Entity rendering type mismatch detected!");
			return false;
		}

		return true;
	}

	protected String name = null;
	protected String groupName = null;
	protected Profile profile = null;
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

	public void createQualityIcon(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		Optional<String> quality = entity.fromBlueprint().quality;
		if (quality.isEmpty() || quality.get().equals("normal")) {
			return;
		}

		Optional<IconDef> optDef = entity.getFactory().getProfile().getIconManager().lookupQuality(quality.get());
		if (optDef.isEmpty()) {
			return;
		}

		ImageDef def = optDef.get();
		MapRect3D bounds = getDrawBounds(entity);
		double size = Math.round(Math.min(bounds.getX2() - bounds.getX1(), bounds.getY2() - bounds.getY1())) > 1 ? 0.5
				: 0.25;
		register.accept(new MapIcon(MapPosition.byUnit(bounds.getX1() + size / 2.0, bounds.getY2() - size / 2.0), def,
				size, OptionalDouble.empty(), false, Optional.empty()));
	}

	public void createModuleIcons(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		IconManager iconManager = entity.getFactory().getProfile().getIconManager();

		MapPosition position = entity.getPosition();
		MapRect3D bounds = entity.getBounds();

		List<EntityModule> renderModules = entity.getModules();
		if (!renderModules.isEmpty()) {

			double x = position.getX() - 0.7 * (renderModules.size() / 2.0) + 0.35;
			double y = position.getY() + 0.7;

			for (EntityModule module : renderModules) {
				Optional<IconDef> image = iconManager.lookupItem(module.name);
				if (image.isPresent()) {
					register.accept(new MapIcon(MapPosition.byUnit(x, y), image.get(), 0.5, OptionalDouble.of(0.05),
							true, module.quality));
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

					List<EntityModule> modules2 = beacon.getModules();
					for (EntityModule module : modules2) {
						double amount = beaconModules.getOrDefault(module.name, 0.0);
						amount += distributionEffectivity;
						beaconModules.put(module.name, amount);
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

				for (EntityModule module : renderModules) {
					Optional<IconDef> image = iconManager.lookupItem(module.name);
					if (image.isPresent()) {
						register.accept(new MapIcon(MapPosition.byUnit(x, y), image.get(), 0.25,
								OptionalDouble.of(0.025), true, module.quality));
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

	public Profile getProfile() {
		return profile;
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

	public void setProfile(Profile profile) {
		this.profile = profile;
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

		DataTable table = profile.getFactorioData().getTable();

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

	private String[] cachedEntityTypeAnnotationValues = null;

	/**
	 * Checks if the given prototype's type matches the @EntityType annotation on the rendering class.
	 * Returns true if they match, false otherwise. Returns false if annotation is missing.
	 * The annotation value is cached for performance.
	 */
	public boolean isEntityTypeMatch(EntityPrototype proto) {
		if (cachedEntityTypeAnnotationValues == null) {
			EntityType annotation = getClass().getAnnotation(EntityType.class);
			if (annotation == null) {
				return false;
			} else {
				cachedEntityTypeAnnotationValues = annotation.value();
			}
		}
		String actualType = proto.getType();
		return Arrays.asList(cachedEntityTypeAnnotationValues).contains(actualType);
	}

}
