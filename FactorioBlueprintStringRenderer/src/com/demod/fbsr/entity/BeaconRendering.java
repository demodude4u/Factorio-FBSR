package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.Utils;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.SpriteDef;
import com.demod.fbsr.WorldMap;

public class BeaconRendering extends EntityRendererFactory {

	private ArrayList<SpriteDef> protoAnimations;
	private double protoSupplyAreaDistance;
	private double protoDistributionEffectivity;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {
		register.accept(RenderUtils.spriteDefRenderer(protoAnimations, entity, protoSelectionBox));
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		protoAnimations = new ArrayList<>();
		LuaValue animationList = prototype.lua().get("graphics_set").get("animation_list");
		if (!animationList.isnil()) {
			Utils.forEach(animationList.checktable(), (i, l) -> {
				List<SpriteDef> animation = RenderUtils.getSpritesFromAnimation(l.get("animation"));
				for (SpriteDef s : animation)
					s.withOrder(i.toint());
				protoAnimations.addAll(animation);
			});
		} else {
			protoAnimations.addAll(RenderUtils.getSpritesFromAnimation(prototype.lua().get("base_picture")));
			protoAnimations.addAll(RenderUtils.getSpritesFromAnimation(prototype.lua().get("animation")));
		}

		protoSupplyAreaDistance = prototype.lua().get("supply_area_distance").todouble();
		protoDistributionEffectivity = prototype.lua().get("distribution_effectivity").todouble();
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity) {
		Point2D.Double pos = entity.getPosition();

		Rectangle2D.Double supplyBounds = protoSelectionBox.createRect();
		supplyBounds.x += pos.x - protoSupplyAreaDistance;
		supplyBounds.y += pos.y - protoSupplyAreaDistance;
		supplyBounds.width += protoSupplyAreaDistance * 2;
		supplyBounds.height += protoSupplyAreaDistance * 2;

		// XXX jank
		entity.json().put("distribution_effectivity", protoDistributionEffectivity);

		double x2 = supplyBounds.x + supplyBounds.width;
		double y2 = supplyBounds.y + supplyBounds.height;
		Point2D.Double bPos = new Point2D.Double();
		for (bPos.x = supplyBounds.x + 0.5; bPos.x < x2; bPos.x++) {
			for (bPos.y = supplyBounds.y + 0.5; bPos.y < y2; bPos.y++) {
				map.setBeaconed(bPos, entity);
			}
		}
	}
}
