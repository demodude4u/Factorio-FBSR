package com.demod.fbsr.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Renderer.Layer;
import com.demod.fbsr.WorldMap;

public class CurvedRailRendering extends TypeRendererFactory {
	private static final String[] railNames = { //
			"curved_rail_vertical_left_bottom", //
			"curved_rail_vertical_right_bottom", //
			"curved_rail_horizontal_left_top", //
			"curved_rail_horizontal_left_bottom", //
			"curved_rail_vertical_right_top", //
			"curved_rail_vertical_left_top", //
			"curved_rail_horizontal_right_bottom", //
			"curved_rail_horizontal_right_top", //
	};

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		String railName = railNames[entity.getDirection().ordinal()];
		LuaValue pictureRailLua = prototype.lua().get("pictures").get(railName);
		for (Entry<String, Layer> entry : StraightRailRendering.railLayers.entrySet()) {
			Sprite railLayerSprite = RenderUtils.getSpriteFromAnimation(pictureRailLua.get(entry.getKey()).get("sheet"));
			register.accept(RenderUtils.spriteRenderer(entry.getValue(), railLayerSprite, entity, prototype));
		}

		if (map.getDebug().rail) {
			register.accept(new Renderer(Layer.DEBUG_RA, entity.getPosition()) {
				@Override
				public void render(Graphics2D g) {
					Point2D.Double pos = entity.getPosition();
					g.setColor(Color.cyan);
					g.fill(new Ellipse2D.Double(pos.x - 0.2, pos.y - 0.2, 0.4, 0.4));
				}
			});
		}
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, EntityPrototype prototype) {
	}
}
