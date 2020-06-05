package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
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

public class MiningDrillRendering extends EntityRendererFactory {

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		if (entity.getName().equals("pumpjack")) {
			List<Sprite> baseSprites = RenderUtils.getSpritesFromAnimation(prototype.lua().get("base_picture"));
			baseSprites.forEach(s -> s.source.x = s.source.width * entity.getDirection().cardinal());
			List<Sprite> jackSprites = RenderUtils
					.getSpritesFromAnimation(prototype.lua().get("animations").get("north"));

			register.accept(RenderUtils.spriteRenderer(baseSprites, entity, prototype));
			register.accept(RenderUtils.spriteRenderer(jackSprites, entity, prototype));
		} else {
			List<Sprite> sprites = RenderUtils.getSpritesFromAnimation(prototype.lua().get("animations"),
					entity.getDirection());
			register.accept(RenderUtils.spriteRenderer(sprites, entity, prototype));
		}
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, EntityPrototype prototype) {
		if (entity.getName().equals("pumpjack")) {

			List<Point2D.Double> positions = new ArrayList<>();
			Utils.forEach(prototype.lua().get("output_fluid_box").get("pipe_connections").get(1).get("positions"),
					l -> {
						positions.add(Utils.parsePoint2D(l));
					});

			Point2D.Double entityPos = entity.getPosition();
			Point2D.Double pipePos = entity.getDirection().back()
					.offset(positions.get(entity.getDirection().cardinal()));
			pipePos.x += entityPos.x;
			pipePos.y += entityPos.y;

			map.setPipe(pipePos, entity.getDirection());
		}
	}
}
