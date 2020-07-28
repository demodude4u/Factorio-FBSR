package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
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
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;

public class MiningDrillRendering extends EntityRendererFactory {

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		List<Sprite> animations = new ArrayList<>();
		LuaValue graphicsSet = prototype.lua().get("graphics_set");

		if (!prototype.lua().get("base_picture").isnil()) {
			List<Sprite> baseSprites = RenderUtils.getSpritesFromAnimation(prototype.lua().get("base_picture"));
			baseSprites.forEach(s -> s.source.x = s.source.width * entity.getDirection().cardinal());
			animations.addAll(baseSprites);
		}

		if (!graphicsSet.isnil()) {
			if (!graphicsSet.checktable().get("idle_animation").isnil())
				animations.addAll(RenderUtils.getSpritesFromAnimation(graphicsSet.checktable().get("idle_animation"),
						entity.getDirection()));
			else if (!graphicsSet.checktable().get("animation").isnil())
				animations.addAll(RenderUtils.getSpritesFromAnimation(graphicsSet.checktable().get("animation"),
						entity.getDirection()));

			if (!graphicsSet.checktable().get("working_visualisations").isnil()) {
				Utils.forEach(graphicsSet.checktable().get("working_visualisations").checktable(), (i, l) -> {
					if (!l.get("always_draw").isnil() && l.get("always_draw").checkboolean()) {
						if (!l.get(entity.getDirection().name().toLowerCase() + "_animation").isnil()) {
							List<Sprite> animation = RenderUtils.getSpritesFromAnimation(
									l.get(entity.getDirection().name().toLowerCase() + "_animation"));
							for (Sprite s : animation)
								s.order = i.toint();
							animations.addAll(animation);
						}
						// TODO Handle "animation" and direction + "_position"
					}
				});
			}
		} else if (!prototype.lua().get("animations").isnil()) {
			LuaValue ani = prototype.lua().get("animations");
			if (ani.get(entity.getDirection().name().toLowerCase()).isnil()) {
				animations.addAll(RenderUtils.getSpritesFromAnimation(ani.get("north")));
			} else
				animations.addAll(RenderUtils.getSpritesFromAnimation(ani, entity.getDirection()));
		}

		register.accept(RenderUtils.spriteRenderer(animations, entity, prototype));
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
