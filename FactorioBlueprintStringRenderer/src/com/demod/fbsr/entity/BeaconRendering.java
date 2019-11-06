package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.Utils;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;

public class BeaconRendering extends EntityRendererFactory {
	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		Sprite baseSprite = RenderUtils.getSpriteFromAnimation(prototype.lua().get("base_picture"));
		Sprite antennaSpriteShadow = RenderUtils.getSpriteFromAnimation(prototype.lua().get("animation_shadow"));
		Sprite antennaSprite = RenderUtils.getSpriteFromAnimation(prototype.lua().get("animation"));
		register.accept(RenderUtils.spriteRenderer(baseSprite, entity, prototype));
		register.accept(RenderUtils.spriteRenderer(antennaSpriteShadow, entity, prototype));
		register.accept(RenderUtils.spriteRenderer(antennaSprite, entity, prototype));
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, EntityPrototype prototype) {
		Point2D.Double pos = entity.getPosition();

		double supplyAreaDistance = prototype.lua().get("supply_area_distance").todouble();
		Rectangle2D.Double supplyBounds = Utils.parseRectangle(prototype.lua().get("selection_box"));
		supplyBounds.x += pos.x - supplyAreaDistance;
		supplyBounds.y += pos.y - supplyAreaDistance;
		supplyBounds.width += supplyAreaDistance * 2;
		supplyBounds.height += supplyAreaDistance * 2;

		// XXX
		entity.json().put("distribution_effectivity", prototype.lua().get("distribution_effectivity").todouble());

		double x2 = supplyBounds.x + supplyBounds.width;
		double y2 = supplyBounds.y + supplyBounds.height;
		Point2D.Double bPos = new Point2D.Double();
		int debugCount = 0;
		for (bPos.x = supplyBounds.x + 0.5; bPos.x < x2; bPos.x++) {
			for (bPos.y = supplyBounds.y + 0.5; bPos.y < y2; bPos.y++) {
				map.setBeaconed(bPos, entity);
				debugCount++;
			}
		}
	}
}
