package com.demod.fbsr.render;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.Utils;
import com.demod.factorio.prototype.DataPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.WorldMap;

public class MiningDrillRendering extends TypeRendererFactory {

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			DataPrototype prototype) {
		if (entity.getName().equals("pumpjack")) {
			Sprite baseSprite = getSpriteFromAnimation(prototype.lua().get("base_picture").get("sheet"));
			baseSprite.source.x = baseSprite.source.width * entity.getDirection().cardinal();
			Sprite jackSprite = getSpriteFromAnimation(prototype.lua().get("animations").get("north"));

			register.accept(spriteRenderer(baseSprite, entity, prototype));
			register.accept(spriteRenderer(jackSprite, entity, prototype));
		} else {
			List<Sprite> sprites = getSpritesFromAnimation(prototype.lua().get("animations"), entity.getDirection());
			register.accept(spriteRenderer(sprites, entity, prototype));
		}
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, DataPrototype prototype) {
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
