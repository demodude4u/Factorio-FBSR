package com.demod.fbsr;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.Utils;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.factorio.prototype.RecipePrototype;
import com.demod.fbsr.Renderer.Layer;
import com.demod.fbsr.WorldMap.BeaconSource;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.entity.AccumulatorRendering;
import com.demod.fbsr.entity.AmmoTurretRendering;
import com.demod.fbsr.entity.ArithmeticCombinatorRendering;
import com.demod.fbsr.entity.ArtilleryTurretRendering;
import com.demod.fbsr.entity.ArtilleryWagonRendering;
import com.demod.fbsr.entity.AssemblingMachineRendering;
import com.demod.fbsr.entity.BeaconRendering;
import com.demod.fbsr.entity.BoilerRendering;
import com.demod.fbsr.entity.BurnerGeneratorRendering;
import com.demod.fbsr.entity.ConstantCombinatorRendering;
import com.demod.fbsr.entity.ContainerRendering;
import com.demod.fbsr.entity.CurvedRailRendering;
import com.demod.fbsr.entity.DeciderCombinatorRendering;
import com.demod.fbsr.entity.ElectricEnergyInterfaceRendering;
import com.demod.fbsr.entity.ElectricPoleRendering;
import com.demod.fbsr.entity.ElectricTurretRendering;
import com.demod.fbsr.entity.FluidTurretRendering;
import com.demod.fbsr.entity.FurnaceRendering;
import com.demod.fbsr.entity.GateRendering;
import com.demod.fbsr.entity.GeneratorRendering;
import com.demod.fbsr.entity.HeatInterfaceRendering;
import com.demod.fbsr.entity.HeatPipeRendering;
import com.demod.fbsr.entity.InfinityContainerRendering;
import com.demod.fbsr.entity.InserterRendering;
import com.demod.fbsr.entity.LabRendering;
import com.demod.fbsr.entity.LampRendering;
import com.demod.fbsr.entity.LandMineRendering;
import com.demod.fbsr.entity.LegacyCurvedRailRendering;
import com.demod.fbsr.entity.LegacyStraightRailRendering;
import com.demod.fbsr.entity.LinkedBeltRendering;
import com.demod.fbsr.entity.LinkedContainerRendering;
import com.demod.fbsr.entity.Loader1x1Rendering;
import com.demod.fbsr.entity.Loader1x2Rendering;
import com.demod.fbsr.entity.LogisticContainerRendering;
import com.demod.fbsr.entity.MiningDrillRendering;
import com.demod.fbsr.entity.OffshorePumpRendering;
import com.demod.fbsr.entity.PipeRendering;
import com.demod.fbsr.entity.PipeToGroundRendering;
import com.demod.fbsr.entity.PowerSwitchRendering;
import com.demod.fbsr.entity.ProgrammableSpeakerRendering;
import com.demod.fbsr.entity.PumpRendering;
import com.demod.fbsr.entity.RadarRendering;
import com.demod.fbsr.entity.RailChainSignalRendering;
import com.demod.fbsr.entity.RailSignalRendering;
import com.demod.fbsr.entity.ReactorRendering;
import com.demod.fbsr.entity.RoboportRendering;
import com.demod.fbsr.entity.RocketSiloRendering;
import com.demod.fbsr.entity.RollingStockRendering;
import com.demod.fbsr.entity.SolarPanelRendering;
import com.demod.fbsr.entity.SplitterRendering;
import com.demod.fbsr.entity.StorageTankRendering;
import com.demod.fbsr.entity.StraightRailRendering;
import com.demod.fbsr.entity.TrainStopRendering;
import com.demod.fbsr.entity.TransportBeltRendering;
import com.demod.fbsr.entity.UndergroundBeltRendering;
import com.demod.fbsr.entity.WallRendering;
import com.demod.fbsr.fp.FPBoundingBox;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;

public abstract class EntityRendererFactory {

	public static final EntityRendererFactory UNKNOWN = new EntityRendererFactory() {
		Set<String> labeledTypes = new HashSet<>();

		@Override
		public void createModuleIcons(Consumer<Renderer> register, WorldMap map, DataTable table, BSEntity entity) {
		}

		@Override
		public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BSEntity entity) {
			Point2D.Double pos = entity.position.createPoint();
			Rectangle2D.Double bounds = new Rectangle2D.Double(pos.x - 0.5, pos.y - 0.5, 1.0, 1.0);
			register.accept(new Renderer(Layer.OVERLAY3, bounds) {
				@Override
				public void render(Graphics2D g) {
					g.setColor(RenderUtils.withAlpha(
							Color.getHSBColor(new Random(entity.name.hashCode()).nextFloat(), 0.6f, 0.4f), 128));
					g.fill(new Ellipse2D.Double(bounds.x, bounds.y, bounds.width, bounds.height));
					g.setColor(Color.gray);
					g.setFont(new Font("Monospaced", Font.BOLD, 1).deriveFont(1f));
					g.drawString("?", (float) bounds.getCenterX() - 0.25f, (float) bounds.getCenterY() + 0.3f);
				}
			});
			register.accept(new Renderer(Layer.OVERLAY4, bounds) {
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
		public void createWireConnections(Consumer<Renderer> register, WorldMap map, DataTable table, BSEntity entity) {
		}

		@Override
		public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		}

		@Override
		public void populateLogistics(WorldMap map, DataTable dataTable, BSEntity entity) {
		}

		@Override
		public void populateWorldMap(WorldMap map, DataTable dataTable, BSEntity entity) {
			if (!labeledTypes.isEmpty()) {
				labeledTypes.clear();
			}
		}
	};

	private static final Map<String, EntityRendererFactory> byName = new LinkedHashMap<>();
	static {
		byName.put("accumulator", new AccumulatorRendering());
		byName.put("arithmetic-combinator", new ArithmeticCombinatorRendering());
		byName.put("artillery-turret", new ArtilleryTurretRendering());
		byName.put("artillery-wagon", new ArtilleryWagonRendering());
		byName.put("assembling-machine-1", new AssemblingMachineRendering());
		byName.put("assembling-machine-2", new AssemblingMachineRendering());
		byName.put("assembling-machine-3", new AssemblingMachineRendering());
		byName.put("centrifuge", new AssemblingMachineRendering());
		byName.put("chemical-plant", new AssemblingMachineRendering());
		byName.put("oil-refinery", new AssemblingMachineRendering());
		byName.put("beacon", new BeaconRendering());
		byName.put("boiler", new BoilerRendering());
		byName.put("heat-exchanger", new BoilerRendering());
		byName.put("burner-generator", new BurnerGeneratorRendering());
		byName.put("cargo-wagon", new RollingStockRendering());
		byName.put("constant-combinator", new ConstantCombinatorRendering());
//		byName.put("blue-chest", new ContainerRendering());
		byName.put("gun-turret", new AmmoTurretRendering());
		byName.put("iron-chest", new ContainerRendering());
//		byName.put("red-chest", new ContainerRendering());
		byName.put("steel-chest", new ContainerRendering());
		byName.put("wooden-chest", new ContainerRendering());
		byName.put("curved-rail-a", new CurvedRailRendering());
		byName.put("curved-rail-b", new CurvedRailRendering());
		byName.put("legacy-curved-rail", new LegacyCurvedRailRendering());
		byName.put("decider-combinator", new DeciderCombinatorRendering());
		byName.put("electric-energy-interface", new ElectricEnergyInterfaceRendering());
		byName.put("big-electric-pole", new ElectricPoleRendering());
		byName.put("medium-electric-pole", new ElectricPoleRendering());
		byName.put("small-electric-pole", new ElectricPoleRendering());
		byName.put("substation", new ElectricPoleRendering());
		byName.put("laser-turret", new ElectricTurretRendering());
		byName.put("flamethrower-turret", new FluidTurretRendering());
		byName.put("fluid-wagon", new RollingStockRendering());
		byName.put("electric-furnace", new FurnaceRendering());
		byName.put("steel-furnace", new FurnaceRendering());
		byName.put("stone-furnace", new FurnaceRendering());
		byName.put("gate", new GateRendering());
		byName.put("steam-engine", new GeneratorRendering());
		byName.put("steam-turbine", new GeneratorRendering());
		byName.put("heat-interface", new HeatInterfaceRendering());
		byName.put("heat-pipe", new HeatPipeRendering());
		byName.put("infinity-chest", new InfinityContainerRendering());
		byName.put("infinity-pipe", new PipeRendering());
		byName.put("burner-inserter", new InserterRendering());
		byName.put("fast-inserter", new InserterRendering());
		byName.put("inserter", new InserterRendering());
		byName.put("long-handed-inserter", new InserterRendering());
		byName.put("bulk-inserter", new InserterRendering());
		byName.put("stack-inserter", new InserterRendering());
		byName.put("lab", new LabRendering());
		byName.put("small-lamp", new LampRendering());
		byName.put("land-mine", new LandMineRendering());
		byName.put("linked-belt", new LinkedBeltRendering());
		byName.put("linked-chest", new LinkedContainerRendering());
		byName.put("express-loader", new Loader1x2Rendering());
		byName.put("fast-loader", new Loader1x2Rendering());
		byName.put("loader", new Loader1x2Rendering());
		byName.put("loader-1x1", new Loader1x1Rendering());
		byName.put("locomotive", new RollingStockRendering());
		byName.put("active-provider-chest", new LogisticContainerRendering());
		byName.put("buffer-chest", new LogisticContainerRendering());
		byName.put("passive-provider-chest", new LogisticContainerRendering());
		byName.put("requester-chest", new LogisticContainerRendering());
		byName.put("storage-chest", new LogisticContainerRendering());
		byName.put("burner-mining-drill", new MiningDrillRendering());
		byName.put("electric-mining-drill", new MiningDrillRendering());
		byName.put("pumpjack", new MiningDrillRendering());
		byName.put("offshore-pump", new OffshorePumpRendering());
		byName.put("pipe", new PipeRendering());
		byName.put("pipe-to-ground", new PipeToGroundRendering());
		byName.put("power-switch", new PowerSwitchRendering());
		byName.put("programmable-speaker", new ProgrammableSpeakerRendering());
		byName.put("pump", new PumpRendering());
		byName.put("radar", new RadarRendering());
		byName.put("rail-chain-signal", new RailChainSignalRendering());
		byName.put("rail-signal", new RailSignalRendering());
		byName.put("nuclear-reactor", new ReactorRendering());
		byName.put("roboport", new RoboportRendering());
		byName.put("rocket-silo", new RocketSiloRendering());
		byName.put("solar-panel", new SolarPanelRendering());
		byName.put("express-splitter", new SplitterRendering());
		byName.put("fast-splitter", new SplitterRendering());
		byName.put("splitter", new SplitterRendering());
		byName.put("storage-tank", new StorageTankRendering());
		byName.put("straight-rail", new StraightRailRendering());
		byName.put("legacy-straight-rail", new LegacyStraightRailRendering());
		byName.put("train-stop", new TrainStopRendering());
		byName.put("express-transport-belt", new TransportBeltRendering());
		byName.put("fast-transport-belt", new TransportBeltRendering());
		byName.put("transport-belt", new TransportBeltRendering());
		byName.put("express-underground-belt", new UndergroundBeltRendering());
		byName.put("fast-underground-belt", new UndergroundBeltRendering());
		byName.put("underground-belt", new UndergroundBeltRendering());
		byName.put("stone-wall", new WallRendering());
	}

	private static final Map<String, Integer> wireConnectionCircuitId = new LinkedHashMap<>();
	static {
		wireConnectionCircuitId.put("circuit_wire_connection_point", 1);
		wireConnectionCircuitId.put("connection_points", 1);
		wireConnectionCircuitId.put("circuit_wire_connection_points", 1);
		wireConnectionCircuitId.put("input_connection_points", 1);
		wireConnectionCircuitId.put("output_connection_points", 2);
	}

	private static boolean prototypesInitialized = false;

	public static EntityRendererFactory forName(String name) {
		return Optional.ofNullable(byName.get(name)).orElse(UNKNOWN);
	}

	public static void initPrototypes(DataTable table) {
		if (prototypesInitialized) {
			return;
		}
		for (Entry<String, EntityRendererFactory> entry : byName.entrySet()) {
			System.out.println("Initializing " + entry.getKey());
			EntityPrototype prototype = table.getEntity(entry.getKey()).get();
			entry.getValue().setPrototype(prototype);
			try {
				entry.getValue().initFromPrototype(table, prototype);
			} catch (Exception e) {
				prototype.debugPrint();
				throw e;
			}
		}
		prototypesInitialized = true;
	}

	private EntityPrototype prototype = null;
	protected FPBoundingBox protoSelectionBox;
	protected boolean protoBeaconed;

	protected void addLogisticWarp(WorldMap map, Point2D.Double gridPos1, Direction cellDir1, Point2D.Double gridPos2,
			Direction cellDir2) {
		map.getOrCreateLogisticGridCell(cellDir1.offset(gridPos1, 0.25)).addWarp(cellDir2.offset(gridPos2, 0.25));
	}

	public void createModuleIcons(Consumer<Renderer> register, WorldMap map, DataTable table, BSEntity entity) {
		Multiset<String> renderModules = RenderUtils.getModules(entity, table);
		if (!renderModules.isEmpty()) {

			register.accept(new Renderer(Layer.OVERLAY3, entity.position.createPoint()) {
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
						BufferedImage image = table.getItem(itemName).map(FactorioData::getIcon)
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

					Multiset<String> modules2 = RenderUtils.getModules(beacon, table);
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

				register.accept(new Renderer(Layer.OVERLAY3, entity.position.createPoint()) {
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
							BufferedImage image = table.getItem(itemName).map(FactorioData::getIcon)
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

	public abstract void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BSEntity entity);

	public void createWireConnections(Consumer<Renderer> register, WorldMap map, DataTable table, BSEntity entity) {
		int entityId = entity.entityNumber;

		// TODO redo this to use the new wires structure

		JSONObject connectionsJson = entity.json().optJSONObject("connections");
		if (connectionsJson != null) {
			Utils.forEach(connectionsJson, (String circuitIdStr, Object connection) -> {
				if (connection instanceof JSONObject) {
					JSONObject connectionJson = (JSONObject) connection;
					int circuitId = Integer.parseInt(circuitIdStr);
					Utils.forEach(connectionJson, (String colorName, JSONArray wiresJson) -> {
						Utils.forEach(wiresJson, (JSONObject wireJson) -> {
							int targetCircuitId = wireJson.optInt("circuit_id", 1);
							int targetEntityId = wireJson.getInt("entity_id");

							String key;
							if (entityId == targetEntityId) {
								key = entityId + "|" + colorName;
							} else if (entityId < targetEntityId) {
								key = entityId + "|" + circuitId + "|" + targetEntityId + "|" + targetCircuitId + "|"
										+ colorName;
							} else {
								key = targetEntityId + "|" + targetCircuitId + "|" + entityId + "|" + circuitId + "|"
										+ colorName;
							}

							if (!map.hasWire(key)) {
								map.setWire(key, new SimpleEntry<>(getWirePositionFor(entity, colorName, circuitId),
										new Point2D.Double()));

							} else {
								Entry<Point2D.Double, Point2D.Double> pair = map.getWire(key);

								Point2D.Double p1 = pair.getKey();
								Point2D.Double p2 = pair.getValue();
								p2.setLocation(getWirePositionFor(entity, colorName, circuitId));

								Color color;
								switch (colorName) {
								case "red":
									color = Color.red.darker();
									break;
								case "green":
									color = Color.green.darker();
									break;
								default:
									System.err.println("UNKNOWN COLOR NAME: " + colorName);
									color = Color.magenta;
									break;
								}

								register.accept(RenderUtils.createWireRenderer(p1, p2, color));
							}
						});
					});
				}
			});
		}
	}

	protected void debugPrintContext(BSEntity entity, EntityPrototype prototype) {
		System.out.println("=================================================================");
		System.out.println("=========================== PROTOTYPE ===========================");
		System.out.println("=================================================================");
		Utils.debugPrintLua(prototype.lua());
		System.out.println("=================================================================");
		System.out.println("============================ ENTITY =============================");
		System.out.println("=================================================================");
		Utils.debugPrintJson(entity.getDebugJson());
	}

	public EntityPrototype getPrototype() {
		return prototype;
	}

	protected Point2D.Double getWirePositionFor(BSEntity entity, String colorName, int circuitId) {
		// TODO find a way to preload lua

		LuaValue connectionPointLua = wireConnectionCircuitId.entrySet().stream().filter(e -> e.getValue() == circuitId)
				.map(e -> prototype.lua().get(e.getKey())).filter(l -> !l.isnil()).findAny().get();

		if (connectionPointLua.get("wire").isnil()) {
			connectionPointLua = connectionPointLua.get(entity.direction.cardinal() + 1);
		}

		Point2D.Double pos = entity.position.createPoint();
		Point2D.Double offset;
		offset = Utils.parsePoint2D(connectionPointLua.get("wire").get(colorName));

		return new Point2D.Double(pos.x + offset.x, pos.y + offset.y);
	}

	public abstract void initFromPrototype(DataTable dataTable, EntityPrototype prototype);

	public void populateLogistics(WorldMap map, DataTable dataTable, BSEntity entity) {
		// default do nothing
	}

	public void populateWorldMap(WorldMap map, DataTable dataTable, BSEntity entity) {
		// default do nothing
	}

	protected void setLogisticAcceptFilter(WorldMap map, Point2D.Double gridPos, Direction cellDir,
			Direction acceptFilter) {
		LogisticGridCell cell = map.getOrCreateLogisticGridCell(cellDir.offset(gridPos, 0.25));
		cell.setAcceptFilter(Optional.of(acceptFilter));
	}

	protected void setLogisticMachine(WorldMap map, DataTable dataTable, BSEntity entity, RecipePrototype recipe) {
		Point2D.Double entityPos = entity.position.createPoint();
		Rectangle2D.Double box = protoSelectionBox.createRect();
		double xStart = entityPos.x + box.x;
		double yStart = entityPos.y + box.y;
		double xEnd = xStart + box.width;
		double yEnd = yStart + box.height;

		Set<String> inputs = recipe.getInputs().keySet().stream().filter(k -> dataTable.getItem(k).isPresent())
				.collect(Collectors.toSet());
		Set<String> outputs = recipe.getOutputs().keySet().stream().filter(k -> dataTable.getItem(k).isPresent())
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
