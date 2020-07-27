package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
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
import com.demod.fbsr.Renderer.Layer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;

public class ArtilleryTurretRendering extends EntityRendererFactory {
	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		Point2D.Double baseShift = Utils.parsePoint2D(prototype.lua().get("base_shift"));

		List<Sprite> baseSprites = RenderUtils.getSpritesFromAnimation(prototype.lua().get("base_picture"));
		RenderUtils.shiftSprites(baseSprites, baseShift);
		register.accept(RenderUtils.spriteRenderer(baseSprites, entity, prototype));

		int fileNameSelector = entity.getDirection().ordinal() * 2 + 1;

		LuaValue cannonBarrel = prototype.lua().get("cannon_barrel_pictures");
		List<Sprite> cannonBarrelSprites = RenderUtils.getSpritesFromAnimation(cannonBarrel, fileNameSelector);
		RenderUtils.shiftSprites(cannonBarrelSprites, baseShift);
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY2, cannonBarrelSprites, entity, prototype));

		LuaValue cannonBase = prototype.lua().get("cannon_base_pictures");
		List<Sprite> cannonBaseSprites = RenderUtils.getSpritesFromAnimation(cannonBase, fileNameSelector);
		RenderUtils.shiftSprites(cannonBaseSprites, baseShift);
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY2, cannonBaseSprites, entity, prototype));
	}
}
