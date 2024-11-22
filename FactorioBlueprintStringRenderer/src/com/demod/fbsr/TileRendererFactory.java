package com.demod.fbsr;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.prototype.TilePrototype;
import com.demod.fbsr.Renderer.Layer;
import com.demod.fbsr.bs.BSTile;

public class TileRendererFactory {

	public static final TileRendererFactory UNKNOWN = new TileRendererFactory() {
		Set<String> labeledTypes = new HashSet<>();

		@Override
		public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BSTile tile) {
			Point2D.Double pos = tile.position.createPoint();
			Rectangle2D.Double bounds = new Rectangle2D.Double(pos.x + 0.25, pos.y + 0.25, 0.5, 0.5);
			float randomFactor = new Random(tile.name.hashCode()).nextFloat();
			register.accept(new Renderer(Layer.TILE3, bounds) {
				@Override
				public void render(Graphics2D g) {
					g.setColor(RenderUtils.withAlpha(Color.getHSBColor(randomFactor, 0.6f, 0.4f), 128));
					g.fill(new Ellipse2D.Double(bounds.x, bounds.y, bounds.width, bounds.height));
					g.setColor(Color.gray);
					g.setFont(new Font("Monospaced", Font.BOLD, 1).deriveFont(0.5f));
					g.drawString("?", (float) bounds.getCenterX() - 0.125f, (float) bounds.getCenterY() + 0.15f);
				}
			});
			register.accept(new Renderer(Layer.OVERLAY4, bounds) {
				@Override
				public void render(Graphics2D g) {
					if (labeledTypes.add(tile.name)) {
						g.setFont(g.getFont().deriveFont(0.4f));
						float textX = (float) bounds.x;
						float textY = (float) (bounds.y + bounds.height * randomFactor);
						g.setColor(Color.darkGray);
						g.drawString(tile.name, textX + 0.05f, textY + 0.05f);
						g.setColor(Color.white);
						g.drawString(tile.name, textX, textY);
					}
				}
			});
		}

		@Override
		public void populateWorldMap(WorldMap map, DataTable dataTable, BSTile tile) {
			if (!labeledTypes.isEmpty()) {
				labeledTypes.clear();
			}
		}
	};

	private static Map<String, TileRendererFactory> byName = new HashMap<>();

	static {
		// TODO determine which ones don't go in a blueprint...
		byName.put("acid-refined-concrete", new TileRendererFactory());
		byName.put("black-refined-concrete", new TileRendererFactory());
		byName.put("blue-refined-concrete", new TileRendererFactory());
		byName.put("brown-refined-concrete", new TileRendererFactory());
		byName.put("concrete", new TileRendererFactory());
		byName.put("cyan-refined-concrete", new TileRendererFactory());
		byName.put("deepwater", new TileRendererFactory());
		byName.put("deepwater-green", new TileRendererFactory());
		byName.put("dirt-1", new TileRendererFactory());
		byName.put("dirt-2", new TileRendererFactory());
		byName.put("dirt-3", new TileRendererFactory());
		byName.put("dirt-4", new TileRendererFactory());
		byName.put("dirt-5", new TileRendererFactory());
		byName.put("dirt-6", new TileRendererFactory());
		byName.put("dirt-7", new TileRendererFactory());
		byName.put("dry-dirt", new TileRendererFactory());
		byName.put("grass-1", new TileRendererFactory());
		byName.put("grass-2", new TileRendererFactory());
		byName.put("grass-3", new TileRendererFactory());
		byName.put("grass-4", new TileRendererFactory());
		byName.put("green-refined-concrete", new TileRendererFactory());
		byName.put("hazard-concrete-left", new TileRendererFactory());
		byName.put("hazard-concrete-right", new TileRendererFactory());
		byName.put("lab-dark-1", new TileRendererFactory());
		byName.put("lab-dark-2", new TileRendererFactory());
		byName.put("lab-white", new TileRendererFactory());
		byName.put("landfill", new TileRendererFactory());
		byName.put("nuclear-ground", new TileRendererFactory());
		byName.put("orange-refined-concrete", new TileRendererFactory());
		byName.put("out-of-map", new TileRendererFactory());
		byName.put("pink-refined-concrete", new TileRendererFactory());
		byName.put("purple-refined-concrete", new TileRendererFactory());
		byName.put("red-desert-0", new TileRendererFactory());
		byName.put("red-desert-1", new TileRendererFactory());
		byName.put("red-desert-2", new TileRendererFactory());
		byName.put("red-desert-3", new TileRendererFactory());
		byName.put("red-refined-concrete", new TileRendererFactory());
		byName.put("refined-concrete", new TileRendererFactory());
		byName.put("refined-hazard-concrete-left", new TileRendererFactory());
		byName.put("refined-hazard-concrete-right", new TileRendererFactory());
		byName.put("sand-1", new TileRendererFactory());
		byName.put("sand-2", new TileRendererFactory());
		byName.put("sand-3", new TileRendererFactory());
		byName.put("stone-path", new TileRendererFactory());
		byName.put("tile-unknown", new TileRendererFactory());
		byName.put("tutorial-grid", new TileRendererFactory());
		byName.put("water", new TileRendererFactory());
		byName.put("water-green", new TileRendererFactory());
		byName.put("water-mud", new TileRendererFactory());
		byName.put("water-shallow", new TileRendererFactory());
		byName.put("water-wube", new TileRendererFactory());
		byName.put("yellow-refined-concrete", new TileRendererFactory());
	}

	public static final double tileSize = 32.0;

	private static boolean prototypesInitialized = false;

	public static TileRendererFactory forName(String name) {
		return Optional.ofNullable(byName.get(name)).orElse(UNKNOWN);
	}

	public static void initPrototypes(DataTable table) {
		if (prototypesInitialized) {
			return;
		}
		for (Entry<String, TileRendererFactory> entry : byName.entrySet()) {
			System.out.println("Initializing " + entry.getKey());
			TilePrototype prototype = table.getTile(entry.getKey()).get();
			entry.getValue().setPrototype(prototype);
			entry.getValue().initFromPrototype(table, prototype);
		}
		prototypesInitialized = true;
	}

	private final Random rand = new Random();

	protected TilePrototype prototype;

	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BSTile tile) {
		LuaValue sheetLua = prototype.lua().get("variants").get("material_background");
		if (sheetLua.isnil()) {
			sheetLua = prototype.lua().get("variants").get("main").get(1);
		}
		Sprite sprite = new Sprite();
		sprite.bounds = new Rectangle2D.Double(-0.5, -0.5, 1.0, 1.0);
		sprite.image = FactorioData.getModImage(sheetLua.get("picture").tojstring());
		sprite.source = new Rectangle(0, 0, 32, 32);
		sprite.source.x = rand.nextInt(sheetLua.get("count").toint()) * sprite.source.width;

		register.accept(RenderUtils.spriteRenderer(Layer.TILE1, sprite, tile));
	}

	public TilePrototype getPrototype() {
		return prototype;
	}

	public void initFromPrototype(DataTable table, TilePrototype prototype) {

	}

	public void populateWorldMap(WorldMap map, DataTable dataTable, BSTile tile) {
		// default do nothing
	}

	public void setPrototype(TilePrototype prototype) {
		this.prototype = prototype;
	}
}
