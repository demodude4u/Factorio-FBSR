package com.demod.fbsr.entity;

import java.util.function.Consumer;

import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.ImageDef;
import com.demod.fbsr.Layer;
import com.demod.fbsr.SpriteDef;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.fp.FPAnimation;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;

public class GateRendering extends EntityRendererFactory {
	private static final int FRAME = 0;

	private FPAnimation protoVerticalAnimation;
	private FPAnimation protoHorizontalAnimation;

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		Consumer<SpriteDef> entityRegister = entity.spriteRegister(register, Layer.OBJECT);
		if (isVertical(entity)) {
			protoVerticalAnimation.defineSprites(entityRegister, FRAME);
		} else {
			protoHorizontalAnimation.defineSprites(entityRegister, FRAME);
		}
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		protoVerticalAnimation.defineSprites(register, FRAME);
		protoHorizontalAnimation.defineSprites(register, FRAME);
	}

	@Override
	public void initFromPrototype() {
		protoVerticalAnimation = new FPAnimation(prototype.lua().get("vertical_animation"));
		protoHorizontalAnimation = new FPAnimation(prototype.lua().get("horizontal_animation"));
	}

	private boolean isVertical(MapEntity entity) {
		return entity.getDirection().cardinal() % 2 == 0;
	}

	@Override
	public void populateWorldMap(WorldMap map, MapEntity entity) {
		MapPosition pos = entity.getPosition();
		if (isVertical(entity)) {
			map.setVerticalGate(pos);
		} else {
			map.setHorizontalGate(pos);
		}
	}
}
