package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
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

		Sprite baseSprite = RenderUtils
				.getSpriteFromAnimation(prototype.lua().get("base_picture").get("layers").get(1));
		baseSprite.bounds.x += baseShift.x;
		baseSprite.bounds.y += baseShift.y;
		register.accept(RenderUtils.spriteRenderer(baseSprite, entity, prototype));

		LuaValue cannonBarrel = prototype.lua().get("cannon_barrel_pictures").get("layers").get(1);
		cannonBarrel.set("artillery_direction", entity.getDirection().ordinal()); // XXX
		Sprite cannonBarrelSprite = RenderUtils.getSpriteFromAnimation(cannonBarrel);
		cannonBarrelSprite.bounds.x += baseShift.x;
		cannonBarrelSprite.bounds.y += baseShift.y;
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY2, cannonBarrelSprite, entity, prototype));

		LuaValue cannonBase = prototype.lua().get("cannon_base_pictures").get("layers").get(1);
		cannonBase.set("artillery_direction", entity.getDirection().ordinal()); // XXX
		Sprite cannonBaseSprite = RenderUtils.getSpriteFromAnimation(cannonBase);
		cannonBaseSprite.bounds.x += baseShift.x;
		cannonBaseSprite.bounds.y += baseShift.y;
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY2, cannonBaseSprite, entity, prototype));
	}
}
