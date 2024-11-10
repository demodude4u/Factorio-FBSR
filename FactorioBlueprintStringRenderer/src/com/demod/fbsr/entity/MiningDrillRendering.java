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
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.RenderUtils.SpriteDirDefList;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.SpriteDef;
import com.demod.fbsr.WorldMap;

public class MiningDrillRendering extends EntityRendererFactory {

	private List<Point2D.Double> protoFluidBoxOffsets;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {

		register.accept(RenderUtils.spriteDirDefRenderer(protoDirSprites, entity, protoSelectionBox));
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		LuaValue graphicsSet = prototype.lua().get("graphics_set");

		protoDirSprites = new SpriteDirDefList();

		for (int c = 0; c < 4; c++) {
			Direction dir = Direction.fromCardinal(c);

			List<SpriteDef> animations = new ArrayList<>();

			if (!prototype.lua().get("base_picture").isnil()) {
				List<SpriteDef> baseSprites = RenderUtils.getSpritesFromAnimation(prototype.lua().get("base_picture"));
				baseSprites.forEach(s -> s.getSource().x = s.getSource().width * dir.cardinal());
				animations.addAll(baseSprites);
			}

			if (!graphicsSet.isnil()) {
				if (!graphicsSet.checktable().get("idle_animation").isnil())
					animations.addAll(
							RenderUtils.getSpritesFromAnimation(graphicsSet.checktable().get("idle_animation"), dir));
				else if (!graphicsSet.checktable().get("animation").isnil())
					animations.addAll(
							RenderUtils.getSpritesFromAnimation(graphicsSet.checktable().get("animation"), dir));

				if (!graphicsSet.checktable().get("working_visualisations").isnil()) {
					Utils.forEach(graphicsSet.checktable().get("working_visualisations").checktable(), (i, l) -> {
						if (!l.get("always_draw").isnil() && l.get("always_draw").checkboolean()) {
							if (!l.get(dir.name().toLowerCase() + "_animation").isnil()) {
								List<SpriteDef> animation = RenderUtils
										.getSpritesFromAnimation(l.get(dir.name().toLowerCase() + "_animation"));
								for (SpriteDef s : animation)
									animations.add(s.withOrder(i.toint()));
							}
							// TODO Handle "animation" and direction + "_position"
						}
					});
				}
			} else if (!prototype.lua().get("animations").isnil()) {
				LuaValue ani = prototype.lua().get("animations");
				if (ani.get(dir.name().toLowerCase()).isnil()) {
					animations.addAll(RenderUtils.getSpritesFromAnimation(ani.get("north")));
				} else
					animations.addAll(RenderUtils.getSpritesFromAnimation(ani, dir));
			}

			protoDirSprites.set(dir, animations);
		}

		protoFluidBoxOffsets = new ArrayList<>();
		if (prototype.getName().equals("pumpjack")) {
			Utils.forEach(prototype.lua().get("output_fluid_box").get("pipe_connections").get(1).get("positions"),
					l -> {
						protoFluidBoxOffsets.add(Utils.parsePoint2D(l));
					});
		}
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity) {
		if (entity.getName().equals("pumpjack")) {
			Point2D.Double entityPos = entity.getPosition();
			Point2D.Double pipePos = entity.getDirection().back()
					.offset(protoFluidBoxOffsets.get(entity.getDirection().cardinal()));
			pipePos.x += entityPos.x;
			pipePos.y += entityPos.y;

			map.setPipe(pipePos, entity.getDirection());
		}
	}
}
