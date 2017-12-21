package com.demod.fbsr;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import com.demod.fbsr.entity.AccumulatorRendering;
import com.demod.fbsr.entity.AmmoTurretRendering;
import com.demod.fbsr.entity.ArithmeticCombinatorRendering;
import com.demod.fbsr.entity.ArtilleryTurretRendering;
import com.demod.fbsr.entity.AssemblingMachineRendering;
import com.demod.fbsr.entity.BeaconRendering;
import com.demod.fbsr.entity.BoilerRendering;
import com.demod.fbsr.entity.ConstantCombinatorRendering;
import com.demod.fbsr.entity.ContainerRendering;
import com.demod.fbsr.entity.CurvedRailRendering;
import com.demod.fbsr.entity.DeciderCombinatorRendering;
import com.demod.fbsr.entity.ElectricPoleRendering;
import com.demod.fbsr.entity.ElectricTurretRendering;
import com.demod.fbsr.entity.FluidTurretRendering;
import com.demod.fbsr.entity.FurnaceRendering;
import com.demod.fbsr.entity.GateRendering;
import com.demod.fbsr.entity.GeneratorRendering;
import com.demod.fbsr.entity.HeatPipeRendering;
import com.demod.fbsr.entity.InserterRendering;
import com.demod.fbsr.entity.LabRendering;
import com.demod.fbsr.entity.LampRendering;
import com.demod.fbsr.entity.LandMineRendering;
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
import com.demod.fbsr.entity.SolarPanelRendering;
import com.demod.fbsr.entity.SplitterRendering;
import com.demod.fbsr.entity.StorageTankRendering;
import com.demod.fbsr.entity.StraightRailRendering;
import com.demod.fbsr.entity.TrainStopRendering;
import com.demod.fbsr.entity.TransportBeltRendering;
import com.demod.fbsr.entity.UndergroundBeltRendering;
import com.demod.fbsr.entity.WallRendering;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;

import javafx.util.Pair;

public class EntityRendererFactory {

	private static List<String> defaultProperties = ImmutableList.of("animation", "off_animation", "structure");

	public static final EntityRendererFactory UNKNOWN = new EntityRendererFactory() {
		Set<String> labeledTypes = new HashSet<>();

		@Override
		public void createModuleIcons(Consumer<Renderer> register, WorldMap map, DataTable table,
				BlueprintEntity entity, EntityPrototype prototype) {
		}

		@Override
		public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
				BlueprintEntity entity, EntityPrototype prototype) {
			Point2D.Double pos = entity.getPosition();
			Rectangle2D.Double bounds = new Rectangle2D.Double(pos.x - 0.5, pos.y - 0.5, 1.0, 1.0);
			register.accept(new Renderer(Layer.OVERLAY3, bounds) {
				@Override
				public void render(Graphics2D g) {
					g.setColor(RenderUtils.withAlpha(
							Color.getHSBColor(new Random(entity.getName().hashCode()).nextFloat(), 0.6f, 0.4f), 128));
					g.fill(new Ellipse2D.Double(bounds.x, bounds.y, bounds.width, bounds.height));
					g.setColor(Color.gray);
					g.setFont(new Font("Monospaced", Font.BOLD, 1).deriveFont(1f));
					g.drawString("?", (float) bounds.getCenterX() - 0.25f, (float) bounds.getCenterY() + 0.3f);
				}
			});
			register.accept(new Renderer(Layer.OVERLAY4, bounds) {
				@Override
				public void render(Graphics2D g) {
					if (labeledTypes.add(entity.getName())) {
						g.setFont(new Font("Monospaced", Font.BOLD, 1).deriveFont(0.4f));
						float textX = (float) bounds.x;
						float textY = (float) (bounds.y
								+ bounds.height * new Random(entity.getName().hashCode()).nextFloat());
						g.setColor(Color.darkGray);
						g.drawString(entity.getName(), textX + 0.05f, textY + 0.05f);
						g.setColor(Color.white);
						g.drawString(entity.getName(), textX, textY);
					}
				}
			});
		}

		@Override
		public void createWireConnections(Consumer<Renderer> register, WorldMap map, DataTable table,
				BlueprintEntity entity, EntityPrototype prototype) {
		}

		@Override
		public void populateLogistics(WorldMap map, DataTable dataTable, BlueprintEntity entity,
				EntityPrototype prototype) {
		}

		@Override
		public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity,
				EntityPrototype prototype) {
			if (!labeledTypes.isEmpty()) {
				labeledTypes.clear();
			}
		}
	};

	private static Map<String, EntityRendererFactory> byType = new HashMap<>();
	static {
		byType.put("accumulator", new AccumulatorRendering());
		byType.put("ammo-turret", new AmmoTurretRendering());
		byType.put("arithmetic-combinator", new ArithmeticCombinatorRendering());
		byType.put("artillery-turret", new ArtilleryTurretRendering());
		byType.put("assembling-machine", new AssemblingMachineRendering());
		byType.put("beacon", new BeaconRendering());
		byType.put("boiler", new BoilerRendering());
		byType.put("constant-combinator", new ConstantCombinatorRendering());
		byType.put("container", new ContainerRendering());
		byType.put("curved-rail", new CurvedRailRendering());
		byType.put("decider-combinator", new DeciderCombinatorRendering());
		byType.put("electric-pole", new ElectricPoleRendering());
		byType.put("electric-turret", new ElectricTurretRendering());
		byType.put("fluid-turret", new FluidTurretRendering());
		byType.put("furnace", new FurnaceRendering());
		byType.put("gate", new GateRendering());
		byType.put("generator", new GeneratorRendering());
		byType.put("heat-pipe", new HeatPipeRendering());
		byType.put("inserter", new InserterRendering());
		byType.put("lab", new LabRendering());
		byType.put("lamp", new LampRendering());
		byType.put("land-mine", new LandMineRendering());
		byType.put("logistic-container", new LogisticContainerRendering());
		byType.put("mining-drill", new MiningDrillRendering());
		byType.put("offshore-pump", new OffshorePumpRendering());
		byType.put("pipe", new PipeRendering());
		byType.put("pipe-to-ground", new PipeToGroundRendering());
		byType.put("power-switch", new PowerSwitchRendering());
		byType.put("programmable-speaker", new ProgrammableSpeakerRendering());
		byType.put("pump", new PumpRendering());
		byType.put("radar", new RadarRendering());
		byType.put("rail-chain-signal", new RailChainSignalRendering());
		byType.put("rail-signal", new RailSignalRendering());
		byType.put("reactor", new ReactorRendering());
		byType.put("roboport", new RoboportRendering());
		byType.put("rocket-silo", new RocketSiloRendering());
		byType.put("solar-panel", new SolarPanelRendering());
		byType.put("splitter", new SplitterRendering());
		byType.put("storage-tank", new StorageTankRendering());
		byType.put("straight-rail", new StraightRailRendering());
		byType.put("train-stop", new TrainStopRendering());
		byType.put("transport-belt", new TransportBeltRendering());
		byType.put("underground-belt", new UndergroundBeltRendering());
		byType.put("wall", new WallRendering());
	}

	private static final Map<String, Integer> wireConnectionCircuitId = new LinkedHashMap<>();
	static {
		wireConnectionCircuitId.put("circuit_wire_connection_point", 1);
		wireConnectionCircuitId.put("connection_points", 1);
		wireConnectionCircuitId.put("circuit_wire_connection_points", 1);
		wireConnectionCircuitId.put("input_connection_points", 1);
		wireConnectionCircuitId.put("output_connection_points", 2);
	}

	public static EntityRendererFactory forType(String type) {
		return Optional.ofNullable(byType.get(type)).orElse(UNKNOWN);
	}

	protected void addLogisticWarp(WorldMap map, Double gridPos1, Direction cellDir1, Double gridPos2,
			Direction cellDir2) {
		map.getOrCreateLogisticGridCell(cellDir1.offset(gridPos1, 0.25)).addWarp(cellDir2.offset(gridPos2, 0.25));
	}

	public void createModuleIcons(Consumer<Renderer> register, WorldMap map, DataTable table, BlueprintEntity entity,
			EntityPrototype prototype) {
		if (entity.json().has("items")) {
			Multiset<String> modules = LinkedHashMultiset.create();

			Object itemsJson = entity.json().get("items");
			if (itemsJson instanceof JSONObject) {
				Utils.forEach(entity.json().getJSONObject("items"), (String itemName, Integer count) -> {
					modules.add(itemName, count);
				});
			} else if (itemsJson instanceof JSONArray) {
				Utils.<JSONObject>forEach(entity.json().getJSONArray("items"), j -> {
					modules.add(j.getString("item"), j.getInt("count"));
				});
			}

			register.accept(new Renderer(Layer.OVERLAY3, entity.getPosition()) {
				final double spacing = 0.7;
				final double shadow = 0.6;
				final double size = 0.5;
				final double vpad = 0.7;

				@Override
				public void render(Graphics2D g) {
					Point2D.Double pos = entity.getPosition();
					Rectangle2D.Double box = prototype.getSelectionBox();

					double startX = pos.x + box.x + box.width / 2.0 - spacing * (modules.size() / 2.0) + spacing / 2.0;
					double startY = pos.y + box.y + box.height - vpad;

					Rectangle2D.Double shadowBox = new Rectangle2D.Double(startX - shadow / 2.0, startY - shadow / 2.0,
							shadow, shadow);
					Rectangle2D.Double spriteBox = new Rectangle2D.Double(startX - size / 2.0, startY - size / 2.0,
							size, size);

					for (String itemName : modules) {
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
	}

	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		try {
			List<Sprite> sprites;

			Optional<LuaValue> findSpriteLua = defaultProperties.stream().map(p -> prototype.lua().get(p))
					.filter(l -> l != LuaValue.NIL).findAny();

			if (findSpriteLua.isPresent()) {
				LuaValue spriteLua = findSpriteLua.get();

				boolean hasDir = spriteLua.get(entity.getDirection().name().toLowerCase()) != LuaValue.NIL;
				if (hasDir) {
					spriteLua = spriteLua.get(entity.getDirection().name().toLowerCase());
				}

				sprites = RenderUtils.getSpritesFromAnimation(spriteLua, entity.getDirection());
			} else {
				Sprite sprite = new Sprite();
				sprite.image = FactorioData.getModImage(prototype.lua().get("icon").tojstring());
				sprite.source = new Rectangle(0, 0, sprite.image.getWidth(), sprite.image.getHeight());
				sprite.bounds = (Rectangle2D.Double) prototype.getSelectionBox().clone();
				sprites = ImmutableList.of(sprite);
			}

			register.accept(RenderUtils.spriteRenderer(sprites, entity, prototype));
		} catch (RuntimeException e) {
			debugPrintContext(entity, prototype);
			throw e;
		}
	}

	public void createWireConnections(Consumer<Renderer> register, WorldMap map, DataTable table,
			BlueprintEntity entity, EntityPrototype prototype) {
		int entityId = entity.getId();

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
							if (entityId < targetEntityId) {
								key = entityId + "|" + circuitId + "|" + targetEntityId + "|" + targetCircuitId + "|"
										+ colorName;
							} else {
								key = targetEntityId + "|" + targetCircuitId + "|" + entityId + "|" + circuitId + "|"
										+ colorName;
							}

							if (!map.hasWire(key)) {
								map.setWire(key, new Pair<>(getWirePositionFor(entity, prototype, colorName, circuitId),
										new Point2D.Double()));

							} else {
								Pair<Double, Double> pair = map.getWire(key);

								Double p1 = pair.getKey();
								Double p2 = pair.getValue();
								p2.setLocation(getWirePositionFor(entity, prototype, colorName, circuitId));

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

	protected void debugPrintContext(BlueprintEntity entity, EntityPrototype prototype) {
		System.out.println("=================================================================");
		System.out.println("=========================== PROTOTYPE ===========================");
		System.out.println("=================================================================");
		Utils.debugPrintLua(prototype.lua());
		System.out.println("=================================================================");
		System.out.println("============================ ENTITY =============================");
		System.out.println("=================================================================");
		Utils.debugPrintJson(entity.json());
	}

	protected Point2D.Double getWirePositionFor(BlueprintEntity entity, EntityPrototype prototype, String colorName,
			int circuitId) {
		LuaValue connectionPointLua = wireConnectionCircuitId.entrySet().stream().filter(e -> e.getValue() == circuitId)
				.map(e -> prototype.lua().get(e.getKey())).filter(l -> !l.isnil()).findAny().get();

		if (connectionPointLua.get("wire").isnil()) {
			connectionPointLua = connectionPointLua.get(entity.getDirection().cardinal() + 1);
		}

		Double pos = entity.getPosition();
		Point2D.Double offset;
		offset = Utils.parsePoint2D(connectionPointLua.get("wire").get(colorName));

		return new Point2D.Double(pos.x + offset.x, pos.y + offset.y);
	}

	public void populateLogistics(WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		// default do nothing
	}

	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, EntityPrototype prototype) {
		// default do nothing
	}

	protected void setLogisticAcceptFilter(WorldMap map, Point2D.Double gridPos, Direction cellDir,
			Direction acceptFilter) {
		LogisticGridCell cell = map.getOrCreateLogisticGridCell(cellDir.offset(gridPos, 0.25));
		cell.setAcceptFilter(Optional.of(acceptFilter));
	}

	protected void setLogisticMachine(WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype, RecipePrototype recipe) {
		Point2D.Double entityPos = entity.getPosition();
		Rectangle2D.Double box = prototype.getSelectionBox();
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

}
