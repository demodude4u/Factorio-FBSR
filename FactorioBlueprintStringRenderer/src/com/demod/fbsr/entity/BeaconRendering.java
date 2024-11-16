package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.fp.FPAnimation;
import com.demod.fbsr.fp.FPAnimationElement;

public class BeaconRendering extends EntityRendererFactory {

	public static class FPBeaconGraphicsSet {
		public final List<FPAnimationElement> animationList;

		public FPBeaconGraphicsSet(LuaValue lua) {
			animationList = FPUtils.list(lua.get("animation_list"), FPAnimationElement::new);
		}

		public List<Sprite> createSprites(int frame) {
			List<Sprite> ret = new ArrayList<>();
			for (FPAnimationElement element : animationList) {
				element.animation.createSprites(ret::add, frame);
			}
			return ret;
		}
	}

	private Optional<FPBeaconGraphicsSet> protoGraphicsSet;
	private Optional<FPAnimation> protoBasePicture;
	private double protoSupplyAreaDistance;
	private double protoDistributionEffectivity;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {
		if (protoGraphicsSet.isPresent()) {
			register.accept(
					RenderUtils.spriteRenderer(protoGraphicsSet.get().createSprites(0), entity, protoSelectionBox));
		} else {
			register.accept(
					RenderUtils.spriteRenderer(protoBasePicture.get().createSprites(0), entity, protoSelectionBox));
		}
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {

		protoGraphicsSet = FPUtils.opt(prototype.lua().get("graphics_set"), FPBeaconGraphicsSet::new);
		protoBasePicture = FPUtils.opt(prototype.lua().get("base_picture"), FPAnimation::new);
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
