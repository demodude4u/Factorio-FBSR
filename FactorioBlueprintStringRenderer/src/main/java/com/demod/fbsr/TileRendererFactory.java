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
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.prototype.TilePrototype;
import com.demod.fbsr.Renderer.Layer;

public class TileRendererFactory {

	public static final TileRendererFactory UNKNOWN = new TileRendererFactory() {
		Set<String> labeledTypes = new HashSet<>();

		@Override
		public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintTile tile,
				TilePrototype prototype) {
			Point2D.Double pos = tile.getPosition();
			Rectangle2D.Double bounds = new Rectangle2D.Double(pos.x - 0.25, pos.y - 0.25, 0.5, 0.5);
			float randomFactor = new Random(tile.getName().hashCode()).nextFloat();
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
					if (labeledTypes.add(tile.getName())) {
						g.setFont(g.getFont().deriveFont(0.4f));
						float textX = (float) bounds.x;
						float textY = (float) (bounds.y + bounds.height * randomFactor);
						g.setColor(Color.darkGray);
						g.drawString(tile.getName(), textX + 0.05f, textY + 0.05f);
						g.setColor(Color.white);
						g.drawString(tile.getName(), textX, textY);
					}
				}
			});
		}

		@Override
		public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintTile tile, TilePrototype prototype) {
			if (!labeledTypes.isEmpty()) {
				labeledTypes.clear();
			}
		}
	};

	private static Map<String, TileRendererFactory> byType = new HashMap<>();
	static {
		byType.put("tile", new TileRendererFactory());
	}

	public static final double tileSize = 32.0;

	public static TileRendererFactory forType(String type) {
		return Optional.ofNullable(byType.get(type)).orElse(UNKNOWN);
	}

	private final Random rand = new Random();

	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintTile tile,
			TilePrototype prototype) {
		LuaValue sheetLua = prototype.lua().get("variants").get("material_background");
		if (sheetLua.isnil()) {
			sheetLua = prototype.lua().get("variants").get("main").get(1);
		}
		Sprite sprite = new Sprite();
		sprite.bounds = new Rectangle2D.Double(-0.5, -0.5, 1.0, 1.0);
		sprite.image = FactorioData.getModImage(sheetLua.get("picture").tojstring());
		sprite.source = new Rectangle(0, 0, 32, 32);
		sprite.source.x = rand.nextInt(sheetLua.get("count").toint()) * sprite.source.width;

		register.accept(RenderUtils.spriteRenderer(Layer.TILE1, sprite, tile, prototype));
	}

	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintTile tile, TilePrototype prototype) {
		// default do nothing
	}
}
