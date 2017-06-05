package com.demod.fbsr.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.Direction;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Renderer.Layer;
import com.demod.fbsr.WorldMap;

public class StraightRailRendering extends TypeRendererFactory {

	private static final String[] railNames = { //
			"straight_rail_vertical", // N
			"straight_rail_diagonal_right_top", // NE
			"straight_rail_horizontal", // E
			"straight_rail_diagonal_right_bottom", // SE
			"straight_rail_vertical", // S
			"straight_rail_diagonal_left_bottom", // SW
			"straight_rail_horizontal", // W
			"straight_rail_diagonal_left_top", // NW
	};

	public static final LinkedHashMap<String, Layer> railLayers = new LinkedHashMap<>();
	static {
		railLayers.put("stone_path_background", Layer.RAIL_STONE_BACKGROUND);
		railLayers.put("stone_path", Layer.RAIL_STONE);
		railLayers.put("ties", Layer.RAIL_TIES);
		railLayers.put("backplates", Layer.RAIL_BACKPLATES);
		railLayers.put("metals", Layer.RAIL_METALS);
	}

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {

		String railName = railNames[entity.getDirection().ordinal()];
		LuaValue pictureRailLua = prototype.lua().get("pictures").get(railName);
		for (Entry<String, Layer> entry : railLayers.entrySet()) {
			Sprite railLayerSprite = RenderUtils.getSpriteFromAnimation(pictureRailLua.get(entry.getKey()).get("sheet"));
			register.accept(RenderUtils.spriteRenderer(entry.getValue(), railLayerSprite, entity, prototype));
		}

		if (map.getDebug().rail) {
			register.accept(new Renderer(Layer.DEBUG_RA, entity.getPosition()) {
				@Override
				public void render(Graphics2D g) {
					Point2D.Double pos = entity.getPosition();
					Direction dir = entity.getDirection();

					g.setColor(Color.cyan);
					g.fill(new Ellipse2D.Double(pos.x - 0.2, pos.y - 0.2, 0.4, 0.4));
					g.setStroke(new BasicStroke(2 / 32f));
					g.draw(new Line2D.Double(pos, dir.offset(pos)));

					if (dir.ordinal() == dir.cardinal() * 2) {// H/V

					} else {// Diagonal

					}
				}
			});
		}
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, EntityPrototype prototype) {
	}
}
