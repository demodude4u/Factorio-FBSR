package com.demod.fbsr.entity;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.Utils;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Renderer.Layer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;

public class TrainStopRendering extends EntityRendererFactory {
	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		Color color;
		if (entity.json().has("color")) {
			color = RenderUtils.parseColor(entity.json().getJSONObject("color"));
		} else {
			color = new Color(242, 0, 0, 127);
		}

		List<Sprite> railSprites = RenderUtils.getSpritesFromAnimation(prototype.lua().get("rail_overlay_animations"),
				entity.getDirection());
		List<Sprite> sprites = RenderUtils.getSpritesFromAnimation(prototype.lua().get("animations"),
				entity.getDirection());
		List<Sprite> topSprites = RenderUtils.getSpritesFromAnimation(prototype.lua().get("top_animations"),
				entity.getDirection());
		topSprites.get(1).image = Utils.tintImage(topSprites.get(1).image, color);

		register.accept(RenderUtils.spriteRenderer(Layer.RAIL_BACKPLATES, railSprites, entity, prototype));
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY, sprites, entity, prototype));
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY2, topSprites, entity, prototype));

		if (entity.json().has("station")) {
			String stationName = entity.json().optString("station");
			register.accept(RenderUtils.drawString(Layer.OVERLAY4, entity.getPosition(), Color.white, stationName));
		}
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, EntityPrototype prototype) {
		Point2D.Double pos = entity.getPosition();
		Direction dir = entity.getDirection();

		map.getOrCreateRailNode(dir.offset(dir.left().offset(pos, 2), 0.5)).setStation(dir);
	}
}
