package com.demod.fbsr.render;

import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.WorldMap;

public class GateRendering extends TypeRendererFactory {

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		boolean vertical = isVerticalGate(entity);

		// Point2D.Double pos = entity.getPosition();
		// boolean northPatch = vertical &&
		// map.isWall(Direction.NORTH.offset(pos));
		// boolean eastPatch = !vertical &&
		// map.isWall(Direction.EAST.offset(pos));
		// boolean southPatch = vertical &&
		// map.isWall(Direction.SOUTH.offset(pos));
		// boolean westPatch = !vertical &&
		// map.isWall(Direction.WEST.offset(pos));

		String orientation = vertical ? "vertical" : "horizontal";

		LuaValue spriteLayersLua = prototype.lua().get(orientation + "_animation").get("layers");
		// LuaValue wallPatchLua = prototype.lua().get("wall_patch");
		// LuaValue northPatchLayersLua = null, eastPatchLayersLua = null,
		// westPatchLayersLua = null,
		// southPatchLayersLua = null;

		Sprite spriteShadow = RenderUtils.getSpriteFromAnimation(spriteLayersLua.get(2));
		register.accept(RenderUtils.spriteRenderer(spriteShadow, entity, prototype));

		Sprite baseSprite = RenderUtils.getSpriteFromAnimation(prototype.lua().get(orientation + "_base").get("layers").get(1));
		register.accept(RenderUtils.spriteRenderer(baseSprite, entity, prototype));

		// TODO Patches need to be rendered on the wall pieces!

		// if (northPatch) {
		// northPatchLayersLua = wallPatchLua.get("north").get("layers");
		// Sprite patchSpriteShadow =
		// getSpriteFromAnimation(northPatchLayersLua.get(2));
		// register.accept(spriteRenderer(patchSpriteShadow, entity,
		// prototype));
		// }
		// if (eastPatch) {
		// eastPatchLayersLua = wallPatchLua.get("east").get("layers");
		// Sprite patchSpriteShadow =
		// getSpriteFromAnimation(eastPatchLayersLua.get(2));
		// register.accept(spriteRenderer(patchSpriteShadow, entity,
		// prototype));
		// }
		// if (southPatch) {
		// southPatchLayersLua = wallPatchLua.get("south").get("layers");
		// Sprite patchSpriteShadow =
		// getSpriteFromAnimation(southPatchLayersLua.get(2));
		// register.accept(spriteRenderer(patchSpriteShadow, entity,
		// prototype));
		// }
		// if (westPatch) {
		// westPatchLayersLua = wallPatchLua.get("west").get("layers");
		// Sprite patchSpriteShadow =
		// getSpriteFromAnimation(westPatchLayersLua.get(2));
		// register.accept(spriteRenderer(patchSpriteShadow, entity,
		// prototype));
		// }
		//
		// if (northPatch) {
		// Sprite patchSprite =
		// getSpriteFromAnimation(northPatchLayersLua.get(1));
		// register.accept(spriteRenderer(patchSprite, entity, prototype));
		// }

		Sprite sprite = RenderUtils.getSpriteFromAnimation(spriteLayersLua.get(1));
		register.accept(RenderUtils.spriteRenderer(sprite, entity, prototype));

		// if (eastPatch) {
		// Sprite patchSprite =
		// getSpriteFromAnimation(eastPatchLayersLua.get(1));
		// register.accept(spriteRenderer(patchSprite, entity, prototype));
		// }
		// if (southPatch) {
		// Sprite patchSprite =
		// getSpriteFromAnimation(southPatchLayersLua.get(1));
		// register.accept(spriteRenderer(patchSprite, entity, prototype));
		// }
		// if (westPatch) {
		// Sprite patchSprite =
		// getSpriteFromAnimation(westPatchLayersLua.get(1));
		// register.accept(spriteRenderer(patchSprite, entity, prototype));
		// }

		// TODO Rail Base if over rail
	}

	private boolean isVerticalGate(BlueprintEntity entity) {
		return entity.getDirection().cardinal() % 2 == 0;
	}

	@Override
	public void populateWorldMap(WorldMap map, DataTable dataTable, BlueprintEntity entity, EntityPrototype prototype) {
		if (isVerticalGate(entity)) {
			map.setVerticalGate(entity.getPosition());
		} else {
			map.setHorizontalGate(entity.getPosition());
		}
	}
}
