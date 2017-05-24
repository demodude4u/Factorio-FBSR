package com.demod.fbsr.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

import org.json.JSONArray;
import org.json.JSONObject;
import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.Utils;
import com.demod.factorio.prototype.DataPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.BlueprintEntity.Direction;
import com.demod.fbsr.FBSR;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.render.Renderer.Layer;
import com.google.common.collect.ImmutableList;

import javafx.util.Pair;

public class TypeRendererFactory {

	protected static class Sprite {
		public BufferedImage image;
		public Rectangle source;
		public Rectangle2D.Double bounds;

		public Sprite() {
		}

		public Sprite(Sprite other) {
			image = other.image;
			source = new Rectangle(other.source);
			bounds = new Rectangle2D.Double(other.bounds.x, other.bounds.y, other.bounds.width, other.bounds.height);
		}
	}

	private static List<String> defaultProperties = ImmutableList.of("animation", "off_animation", "structure");
	protected static final TypeRendererFactory DEFAULT = new TypeRendererFactory() {
		Set<String> defaultedTypes = new HashSet<>();

		@Override
		public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
				BlueprintEntity entity, DataPrototype prototype) {
			super.createRenderers(register, map, dataTable, entity, prototype);
			register.accept(new Renderer(Layer.OVERLAY3, entity.getPosition()) {
				@Override
				public void render(Graphics2D g) {
					g.setColor(Utils.withAlpha(
							Color.getHSBColor(new Random(prototype.getName().hashCode()).nextFloat(), 1f, 1f), 128));
					g.fill(new Rectangle2D.Double(bounds.x - 0.5, bounds.y - 0.5, 1, 1));
				}
			});
			register.accept(new Renderer(Layer.OVERLAY4, entity.getPosition()) {
				@Override
				public void render(Graphics2D g) {
					if (defaultedTypes.add(prototype.getType())) {
						g.setFont(g.getFont().deriveFont(0.5f));
						g.setColor(Color.white);
						g.drawString(prototype.getType(), (float) bounds.x, (float) bounds.y);
					}
				}
			});
		}

		@Override
		public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity,
				DataPrototype prototype) {
			super.populateWorldMap(map, dataTable, entity, prototype);

			if (!defaultedTypes.isEmpty()) {
				defaultedTypes.clear();
			}
		}
	};

	private static Map<String, TypeRendererFactory> byType = new HashMap<>();
	static {
		byType.put("", DEFAULT);
		byType.put("accumulator", new AccumulatorRendering());
		byType.put("ammo-turret", new AmmoTurretRendering());
		byType.put("arithmetic-combinator", new ArithmeticCombinatorRendering());
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

	public static final double tileSize = 32.0;

	private static final Map<String, Integer> wireConnectionCircuitId = new LinkedHashMap<>();
	static {
		wireConnectionCircuitId.put("circuit_wire_connection_point", 1);
		wireConnectionCircuitId.put("connection_points", 1);
		wireConnectionCircuitId.put("circuit_wire_connection_points", 1);
		wireConnectionCircuitId.put("input_connection_points", 1);
		wireConnectionCircuitId.put("output_connection_points", 2);
	}

	protected static void drawImageInBounds(BufferedImage image, Rectangle source, Rectangle2D.Double bounds,
			Graphics2D g) {
		AffineTransform pat = g.getTransform();
		g.translate(bounds.x, bounds.y);
		g.scale(bounds.width, bounds.height);
		g.drawImage(image, 0, 0, 1, 1, source.x, source.y, source.x + source.width, source.y + source.height, null);
		g.setTransform(pat);
	}

	protected static void drawSprite(Sprite sprite, Graphics2D g) {
		drawImageInBounds(sprite.image, sprite.source, sprite.bounds, g);
	}

	public static TypeRendererFactory forType(String type) {
		return byType.getOrDefault(type, DEFAULT);
	}

	protected static Sprite getSpriteFromAnimation(LuaValue lua) {
		Sprite ret = new Sprite();
		ret.image = FBSR.getModImage(lua.get("filename"));
		int srcX = lua.get("x").optint(0);
		int srcY = lua.get("y").optint(0);
		int srcWidth = lua.get("width").checkint();
		double width = srcWidth / tileSize;
		int srcHeight = lua.get("height").checkint();
		double height = srcHeight / tileSize;
		Point2D.Double shift = Utils.parsePoint2D(lua.get("shift"));
		ret.source = new Rectangle(srcX, srcY, srcWidth, srcHeight);
		ret.bounds = new Rectangle2D.Double(shift.x - width / 2.0, shift.y - height / 2.0, width, height);
		return ret;
	}

	protected static List<Sprite> getSpritesFromAnimation(LuaValue lua) {
		List<Sprite> sprites = new ArrayList<>();
		LuaValue layersLua = lua.get("layers");
		if (!layersLua.isnil()) {
			Utils.forEach(layersLua.checktable(), l -> {
				sprites.add(getSpriteFromAnimation(l));
			});
			Collections.reverse(sprites);
		} else {
			sprites.add(getSpriteFromAnimation(lua));
		}
		return sprites;
	}

	protected static List<Sprite> getSpritesFromAnimation(LuaValue lua, Direction direction) {
		LuaValue dirLua = lua.get(direction.name().toLowerCase());
		if (!dirLua.isnil()) {
			return getSpritesFromAnimation(dirLua);
		} else {
			return getSpritesFromAnimation(lua);
		}
	}

	protected static Renderer spriteRenderer(Layer layer, List<Sprite> sprites, BlueprintEntity entity,
			DataPrototype prototype) {
		Point2D.Double pos = entity.getPosition();
		for (Sprite sprite : sprites) {
			sprite.bounds.x += pos.x;
			sprite.bounds.y += pos.y;
		}
		// Rectangle2D.Double groundBounds =
		// Utils.parseRectangle(prototype.lua().get("collision_box"));
		Rectangle2D.Double groundBounds = Utils.parseRectangle(prototype.lua().get("selection_box"));
		groundBounds.x += pos.x;
		groundBounds.y += pos.y;
		return new Renderer(layer, groundBounds) {
			@SuppressWarnings("unused")
			private void debugShowBounds(Rectangle2D.Double groundBounds, Graphics2D g) {
				long x = Math.round(groundBounds.getCenterX() * 2);
				long y = Math.round(groundBounds.getCenterY() * 2);
				long w = Math.round(groundBounds.width * 2);
				long h = Math.round(groundBounds.height * 2);

				// System.out.println("x=" + x + " y=" + y + " w=" + w + "
				// h=" + h);

				g.setColor(new Color(255, 255, 255, 64));
				g.draw(groundBounds);

				if (((w / 2) % 2) == (x % 2)) {
					g.setColor(new Color(255, 0, 0, 64));
					g.fill(groundBounds);
				}
				if (((h / 2) % 2) == (y % 2)) {
					g.setColor(new Color(0, 255, 0, 64));
					g.fill(groundBounds);
				}
			}

			@Override
			public void render(Graphics2D g) {
				for (Sprite sprite : sprites) {
					drawSprite(sprite, g);
					// debugShowBounds(groundBounds, g);
				}
			}
		};
	}

	protected static Renderer spriteRenderer(Layer layer, Sprite sprite, BlueprintEntity entity,
			DataPrototype prototype) {
		return spriteRenderer(layer, ImmutableList.of(sprite), entity, prototype);
	}

	protected static Renderer spriteRenderer(List<Sprite> sprites, BlueprintEntity entity, DataPrototype prototype) {
		return spriteRenderer(Layer.ENTITY, sprites, entity, prototype);
	}

	protected static Renderer spriteRenderer(Sprite sprite, BlueprintEntity entity, DataPrototype prototype) {
		return spriteRenderer(Layer.ENTITY, sprite, entity, prototype);
	}

	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			DataPrototype prototype) {
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

				sprites = getSpritesFromAnimation(spriteLua, entity.getDirection());
			} else {
				Sprite sprite = new Sprite();
				sprite.image = FBSR.getModImage(prototype.lua().get("icon"));
				sprite.source = new Rectangle(0, 0, sprite.image.getWidth(), sprite.image.getHeight());
				sprite.bounds = Utils.parseRectangle(prototype.lua().get("selection_box"));
				sprites = ImmutableList.of(sprite);
			}

			register.accept(spriteRenderer(sprites, entity, prototype));
		} catch (RuntimeException e) {
			debugPrintContext(entity, prototype);
			throw e;
		}
	}

	public void createWireConnections(Consumer<Renderer> register, WorldMap map, DataTable table,
			BlueprintEntity entity, DataPrototype prototype) {
		int entityId = entity.getId();

		JSONObject connectionsJson = entity.json().optJSONObject("connections");
		if (connectionsJson != null) {
			Utils.forEach(connectionsJson, (String circuitIdStr, JSONObject connectionJson) -> {
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

							Rectangle2D.Double bounds = new Rectangle2D.Double();
							bounds.setFrameFromDiagonal(p1, p2);

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

							register.accept(new Renderer(Layer.WIRE, bounds) {
								final double drop = 0.6;

								@Override
								public void render(Graphics2D g) {
									Stroke ps = g.getStroke();
									g.setStroke(new BasicStroke(1f / 32f));
									g.setColor(color);

									Path2D.Double path = new Path2D.Double();
									path.moveTo(p1.x, p1.y);
									Point2D.Double mid = new Point2D.Double((p1.x + p2.x) / 2,
											(p1.y + p2.y) / 2 + drop);
									path.curveTo(mid.x, mid.y, mid.x, mid.y, p2.x, p2.y);
									g.draw(path);

									g.setStroke(ps);
								}
							});
						}
					});
				});
			});
		}
	}

	protected void debugPrintContext(BlueprintEntity entity, DataPrototype prototype) {
		System.out.println("=================================================================");
		System.out.println("=========================== PROTOTYPE ===========================");
		System.out.println("=================================================================");
		Utils.debugPrintLua(prototype.lua());
		System.out.println("=================================================================");
		System.out.println("============================ ENTITY =============================");
		System.out.println("=================================================================");
		Utils.debugPrintJson(entity.json());
	}

	protected Point2D.Double getWirePositionFor(BlueprintEntity entity, DataPrototype prototype, String colorName,
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

	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, DataPrototype prototype) {
		// default do nothing
	}

}
