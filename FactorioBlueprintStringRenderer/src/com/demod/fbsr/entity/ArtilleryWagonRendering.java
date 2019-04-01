package com.demod.fbsr.entity;

import java.awt.geom.Point2D.Double;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.Utils;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Renderer.Layer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;

public class ArtilleryWagonRendering extends RollingStockRendering {
	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BlueprintEntity entity,
			EntityPrototype prototype) {
		super.createRenderers(register, map, dataTable, entity, prototype);

		double orientation = entity.json().getDouble("orientation");

		LuaValue cannonBaseLayers = prototype.lua().get("cannon_base_pictures").get("layers");
		LuaValue cannonBaseBody = cannonBaseLayers.get(1);
		Sprite cannonBase = getRotatedSprite(cannonBaseBody, orientation);
		Double cannonBaseShift = Utils.parsePoint2D(prototype.lua().get("cannon_base_shiftings")
				.get(getRotationIndex(prototype.lua().get("pictures").get("layers").get(1), orientation) + 1));
		cannonBase.bounds.x += cannonBaseShift.x;
		cannonBase.bounds.y += cannonBaseShift.y;

		LuaValue cannonBarrelLayers = prototype.lua().get("cannon_barrel_pictures").get("layers");
		LuaValue cannonBarrelBody = cannonBarrelLayers.get(1);
		Sprite cannonBarrel = getRotatedSprite(cannonBarrelBody, orientation);
		cannonBarrel.bounds.x += cannonBaseShift.x;
		cannonBarrel.bounds.y += cannonBaseShift.y;

		// TODO needs proper shadow compositing
		// LuaValue shadow = cannonBaseLayers.get(cannonBaseLayers.length());
		// Sprite cannonBaseShadow = getRotatedSprite(shadow, orientation);
		// cannonBaseShadow.bounds.x += cannonBaseShift.x;
		// cannonBaseShadow.bounds.y += cannonBaseShift.y;

		// register.accept(RenderUtils.spriteRenderer(Layer.ENTITY2, cannonBaseShadow,
		// entity, prototype));
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY2, cannonBarrel, entity, prototype));
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY2, cannonBase, entity, prototype));
	}
}
