package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
import java.util.function.Consumer;

import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.fp.FPAnimation;

public class GateRendering extends EntityRendererFactory<BSEntity> {

	private FPAnimation protoVerticalAnimation;
	private FPAnimation protoHorizontalAnimation;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, BSEntity entity) {
		if (isVertical(entity)) {
			register.accept(RenderUtils.spriteRenderer(protoVerticalAnimation.createSprites(data, 0), entity,
					protoSelectionBox));
		} else {
			register.accept(RenderUtils.spriteRenderer(protoHorizontalAnimation.createSprites(data, 0), entity,
					protoSelectionBox));
		}
	}

	@Override
	public void initFromPrototype() {
		protoVerticalAnimation = new FPAnimation(prototype.lua().get("vertical_animation"));
		protoHorizontalAnimation = new FPAnimation(prototype.lua().get("horizontal_animation"));
	}

	private boolean isVertical(BSEntity entity) {
		return entity.direction.cardinal() % 2 == 0;
	}

	@Override
	public void populateWorldMap(WorldMap map, BSEntity entity) {
		Point2D.Double pos = entity.position.createPoint();
		if (isVertical(entity)) {
			map.setVerticalGate(pos);
		} else {
			map.setHorizontalGate(pos);
		}
	}
}
