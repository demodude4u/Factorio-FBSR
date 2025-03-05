package com.demod.fbsr.entity;

import java.awt.image.BufferedImage;
import java.util.Optional;
import java.util.function.Consumer;

import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.ImageDef;
import com.demod.fbsr.TagManager;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapIcon;
import com.demod.fbsr.map.MapRenderable;

public class SpiderVehicleRendering extends EntityRendererFactory {

	// TODO rendering spider hard, just use icon for now

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		Optional<BufferedImage> icon = TagManager.lookup("entity", entity.fromBlueprint().name);
		if (icon.isPresent()) {
			register.accept(new MapIcon(entity.getPosition(), icon.get(), 2, 0.2, false));
		}
	}

	@Override
	public void initFromPrototype() {
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
	}

}
