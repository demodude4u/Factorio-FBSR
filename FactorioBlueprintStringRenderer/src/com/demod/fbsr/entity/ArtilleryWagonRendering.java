package com.demod.fbsr.entity;

import java.awt.geom.Point2D.Double;
import java.util.ArrayList;
import java.util.List;
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
		Double cannonBaseShift = Utils.parsePoint2D(prototype.lua().get("cannon_base_shiftings")
				.get(getRotationIndex(prototype.lua().get("pictures").get("layers").get(1), orientation) + 1));

		LuaValue cannonBaseLayers = prototype.lua().get("cannon_base_pictures").get("layers");
		List<Sprite> cannonBaseSprites = new ArrayList<Sprite>();
		Utils.forEach(cannonBaseLayers, layerLua -> {
			cannonBaseSprites.add(getRotatedSprite(layerLua, orientation));
		});
		RenderUtils.shiftSprites(cannonBaseSprites, cannonBaseShift);

		LuaValue cannonBarrelLayers = prototype.lua().get("cannon_barrel_pictures").get("layers");
		List<Sprite> cannonBarrelSprites = new ArrayList<Sprite>();
		Utils.forEach(cannonBarrelLayers, layerLua -> {
			cannonBarrelSprites.add(getRotatedSprite(layerLua, orientation));
		});
		RenderUtils.shiftSprites(cannonBarrelSprites, cannonBaseShift);

		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY2, cannonBarrelSprites, entity, prototype));
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY2, cannonBaseSprites, entity, prototype));
	}
}
