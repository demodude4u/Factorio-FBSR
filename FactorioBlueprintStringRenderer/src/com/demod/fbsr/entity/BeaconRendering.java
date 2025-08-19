package com.demod.fbsr.entity;

import java.util.Optional;
import java.util.function.Consumer;

import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Layer;
import com.demod.fbsr.MapUtils;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.SpriteDef;
import com.demod.fbsr.fp.FPAnimation;
import com.demod.fbsr.fp.FPBeaconGraphicsSet;
import com.demod.fbsr.fp.FPBoundingBox;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRect;
import com.demod.fbsr.map.MapRenderable;

@EntityType("beacon")
public class BeaconRendering extends EntityWithOwnerRendering {

	private FPBoundingBox protoSelectionBox;
	private Optional<FPBeaconGraphicsSet> protoGraphicsSet;
	private Optional<FPAnimation> protoBasePicture;
	private Optional<FPAnimation> protoAnimation;
	private double protoSupplyAreaDistance;
	private double protoDistributionEffectivity;

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		Consumer<SpriteDef> spriteRegister = entity.spriteRegister(register, Layer.OBJECT);
		if (protoGraphicsSet.isPresent()) {
			protoGraphicsSet.get().defineSprites(spriteRegister, 0);
		} else if (protoBasePicture.isPresent()) {
			protoBasePicture.get().defineSprites(spriteRegister, 0);
		} else if (protoAnimation.isPresent()) {
			protoAnimation.get().defineSprites(spriteRegister, 0);
		}
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		super.initAtlas(register);

		if (protoGraphicsSet.isPresent()) {
			protoGraphicsSet.get().defineSprites(register, 0);
		} else if (protoBasePicture.isPresent()) {
			protoBasePicture.get().defineSprites(register, 0);
		} else if (protoAnimation.isPresent()) {
			protoAnimation.get().defineSprites(register, 0);
		}
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		protoSelectionBox = new FPBoundingBox(prototype.lua().get("selection_box"));
		protoGraphicsSet = FPUtils.opt(profile, prototype.lua().get("graphics_set"), FPBeaconGraphicsSet::new);
		protoBasePicture = FPUtils.opt(profile, prototype.lua().get("base_picture"), FPAnimation::new);
		protoAnimation = FPUtils.opt(profile, prototype.lua().get("animation"), FPAnimation::new);
		protoSupplyAreaDistance = prototype.lua().get("supply_area_distance").todouble();
		protoDistributionEffectivity = prototype.lua().get("distribution_effectivity").todouble();
	}

	@Override
	public void populateWorldMap(WorldMap map, MapEntity entity) {
		super.populateWorldMap(map, entity);
		
		MapPosition pos = entity.getPosition();
		MapRect supplyBounds = protoSelectionBox.createRect().expandUnit(protoSupplyAreaDistance).add(pos);

		int halfFP = MapUtils.unitToFixedPoint(0.5);
		int tileFP = MapUtils.unitToFixedPoint(1.0);

		int x2FP = supplyBounds.getXFP() + supplyBounds.getWidthFP();
		int y2FP = supplyBounds.getYFP() + supplyBounds.getHeightFP();
		for (int xFP = supplyBounds.getXFP() + halfFP; xFP < x2FP; xFP += tileFP) {
			for (int yFP = supplyBounds.getYFP() + halfFP; yFP < y2FP; yFP += tileFP) {
				map.setBeaconed(MapPosition.byFixedPoint(xFP, yFP), entity, protoDistributionEffectivity);
			}
		}
	}

}
