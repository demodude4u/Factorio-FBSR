package com.demod.fbsr;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.json.JSONObject;

import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.factorio.prototype.RecipePrototype;
import com.demod.fbsr.FBSR.EntityRenderingTuple;
import com.demod.fbsr.WirePoints.WirePoint;
import com.demod.fbsr.WorldMap.BeaconSource;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.fp.FPBoundingBox;
import com.demod.fbsr.legacy.LegacyBlueprintEntity;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EntityRendererFactory<E extends BSEntity> {

	public static final EntityRendererFactory<BSEntity> UNKNOWN = new EntityRendererFactory<BSEntity>() {
		Set<String> labeledTypes = new HashSet<>();

		@Override
		public void createModuleIcons(Consumer<Renderer> register, WorldMap map, BSEntity entity) {
		}

		@Override
		public void createRenderers(Consumer<Renderer> register, WorldMap map, BSEntity entity) {
			Point2D.Double pos = entity.position.createPoint();
			Rectangle2D.Double bounds = new Rectangle2D.Double(pos.x - 0.5, pos.y - 0.5, 1.0, 1.0);
			register.accept(new Renderer(Layer.ENTITY_INFO_ICON_ABOVE, bounds, false) {
				@Override
				public void render(Graphics2D g) {
					g.setColor(RenderUtils.withAlpha(getUnknownColor(entity.name), 128));
					g.fill(new Ellipse2D.Double(bounds.x, bounds.y, bounds.width, bounds.height));
					g.setColor(Color.gray);
					g.setFont(new Font("Monospaced", Font.BOLD, 1).deriveFont(1f));
					g.drawString("?", (float) bounds.getCenterX() - 0.25f, (float) bounds.getCenterY() + 0.3f);
				}
			});
			register.accept(new Renderer(Layer.ENTITY_INFO_TEXT, bounds, false) {
				@Override
				public void render(Graphics2D g) {
					if (labeledTypes.add(entity.name)) {
						g.setFont(new Font("Monospaced", Font.BOLD, 1).deriveFont(0.4f));
						float textX = (float) bounds.x;
						float textY = (float) (bounds.y
								+ bounds.height * new Random(entity.name.hashCode()).nextFloat());
						g.setColor(Color.darkGray);
						g.drawString(entity.name, textX + 0.05f, textY + 0.05f);
						g.setColor(Color.white);
						g.drawString(entity.name, textX, textY);
					}
				}
			});
		}

		@Override
		public Optional<WirePoint> createWirePoint(Consumer<Renderer> register, Point2D.Double position,
				double orientation, int connectionId) {
			return Optional.empty();
		}

		@Override
		public FactorioData getData() {
			return FactorioManager.getBaseData();
		}

		@Override
		public void initFromPrototype() {
		}

		@Override
		public void populateLogistics(WorldMap map, BSEntity entity) {
		}

		@Override
		public void populateWorldMap(WorldMap map, BSEntity entity) {
			if (!labeledTypes.isEmpty()) {
				labeledTypes.clear();
			}
		}
	};
	private static final Logger LOGGER = LoggerFactory.getLogger(EntityRendererFactory.class);

	public static Color getUnknownColor(String name) {
		return Color.getHSBColor(new Random(name.hashCode()).nextFloat(), 0.6f, 0.4f);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void initFactories(List<EntityRendererFactory> factories) {
		for (EntityRendererFactory factory : factories) {
			EntityPrototype prototype = factory.getPrototype();
			try {
				factory.initFromPrototype();
				factory.wirePointsById = new LinkedHashMap<>();
				factory.defineWirePoints(factory.wirePointsById::put, prototype.lua());
			} catch (Exception e) {
				LOGGER.error("ENTITY {} ({})", prototype.getName(), prototype.getType());
				throw e;
			}
		}

		LOGGER.info("Initialized {} entities.", factories.size());
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

	protected Map<Integer, WirePoints> wirePointsById;

	private Class<E> entityClass;

	protected void addLogisticWarp(WorldMap map, Point2D.Double gridPos1, Direction cellDir1, Point2D.Double gridPos2,
			Direction cellDir2) {
		map.getOrCreateLogisticGridCell(cellDir1.offset(gridPos1, 0.25)).addWarp(cellDir2.offset(gridPos2, 0.25));
	}

	public void createModuleIcons(Consumer<Renderer> register, WorldMap map, E entity) {
		DataTable table = data.getTable();

		Multiset<String> renderModules = RenderUtils.getModules(entity);
		if (!renderModules.isEmpty()) {
			register.accept(new Renderer(Layer.ENTITY_INFO_ICON_ABOVE, entity.position.createPoint(), true) {
				final double spacing = 0.7;
				final double shadow = 0.6;
				final double size = 0.5;
				final double vpad = 0.7;

				@Override
				public void render(Graphics2D g) {
					Point2D.Double pos = entity.position.createPoint();
					Rectangle2D.Double box = protoSelectionBox.createRect();

					double startX = pos.x + box.x + box.width / 2.0 - spacing * (renderModules.size() / 2.0)
							+ spacing / 2.0;
					double startY = pos.y + box.y + box.height - vpad;

					Rectangle2D.Double shadowBox = new Rectangle2D.Double(startX - shadow / 2.0, startY - shadow / 2.0,
							shadow, shadow);
					Rectangle2D.Double spriteBox = new Rectangle2D.Double(startX - size / 2.0, startY - size / 2.0,
							size, size);

					for (String itemName : renderModules) {
						g.setColor(new Color(0, 0, 0, 180));
						g.fill(shadowBox);
						BufferedImage image = data.getTable().getItem(itemName).map(data::getWikiIcon)
								.orElse(RenderUtils.EMPTY_IMAGE);
						RenderUtils.drawImageInBounds(image, new Rectangle(0, 0, image.getWidth(), image.getHeight()),
								spriteBox, g);

						shadowBox.x += spacing;
						spriteBox.x += spacing;
					}
				}
			});
		}

		if (protoBeaconed) {
			Point2D.Double pos = entity.position.createPoint();
			Rectangle2D.Double beaconedBounds = protoSelectionBox.createRect();
			beaconedBounds.x += pos.x;
			beaconedBounds.y += pos.y;
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

				register.accept(new Renderer(Layer.ENTITY_INFO_ICON_ABOVE, entity.position.createPoint(), true) {
					final double spacing = 0.3;
					final double shadow = 0.3;
					final double size = 0.25;
					final double vpad = 0.5;

					@Override
					public void render(Graphics2D g) {
						Point2D.Double pos = entity.position.createPoint();
						Rectangle2D.Double box = protoSelectionBox.createRect();

						double startX = pos.x + box.x + box.width / 2.0 - spacing * (modules.size() / 2.0)
								+ spacing / 2.0;
						double startY = pos.y + box.y + vpad;

						Rectangle2D.Double shadowBox = new Rectangle2D.Double(startX - shadow / 2.0,
								startY - shadow / 2.0, shadow, shadow);
						Rectangle2D.Double spriteBox = new Rectangle2D.Double(startX - size / 2.0, startY - size / 2.0,
								size, size);

						for (String itemName : modules) {
							g.setColor(new Color(0, 0, 0, 180));
							g.fill(shadowBox);
							BufferedImage image = table.getItem(itemName).map(data::getWikiIcon)
									.orElse(RenderUtils.EMPTY_IMAGE);
							RenderUtils.drawImageInBounds(image,
									new Rectangle(0, 0, image.getWidth(), image.getHeight()), spriteBox, g);

							shadowBox.x += spacing;
							spriteBox.x += spacing;
						}
					}

				});
			}
		}
	}

	public abstract void createRenderers(Consumer<Renderer> register, WorldMap map, E entity);

	public Optional<WirePoint> createWirePoint(Consumer<Renderer> register, Point2D.Double position, double orientation,
			int connectionId) {
		return Optional.ofNullable(wirePointsById.get(connectionId)).map(wp -> wp.getPoint(position, orientation));
	}

	public void defineWirePoints(BiConsumer<Integer, WirePoints> consumer, LuaTable lua) {

	}

	public FPBoundingBox getBounds() {
		return protoSelectionBox;
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
	public double initWireConnector(Consumer<Renderer> register, E entity, List<EntityRenderingTuple> wired) {
		return 0;
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
		Point2D.Double entityPos = entity.position.createPoint();
		Rectangle2D.Double box = protoSelectionBox.createRect();
		double xStart = entityPos.x + box.x;
		double yStart = entityPos.y + box.y;
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
