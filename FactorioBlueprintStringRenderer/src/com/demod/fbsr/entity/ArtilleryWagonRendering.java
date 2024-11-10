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
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Renderer.Layer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;

public class ArtilleryWagonRendering extends RollingStockRendering {
	private List<Point2D.Double> protoCannonBaseShifts;
	private List<RotationSprites> protoCannonBaseLayers;
	private List<RotationSprites> protoCannonBarrelLayers;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {
		super.createRenderers(register, map, dataTable, entity);

		double orientation = entity.json().getDouble("orientation");

		Point2D.Double cannonBaseShift = protoCannonBaseShifts.get(protoBodySprites.getRotationIndex(orientation));

		List<Sprite> cannonBaseSprites = new ArrayList<Sprite>();
		for (RotationSprites rotationSprites : protoCannonBaseLayers) {
			cannonBaseSprites.add(rotationSprites.getSprite(orientation).createSprite());
		}
		RenderUtils.shiftSprites(cannonBaseSprites, cannonBaseShift);

		List<Sprite> cannonBarrelSprites = new ArrayList<Sprite>();
		for (RotationSprites rotationSprites : protoCannonBarrelLayers) {
			cannonBarrelSprites.add(rotationSprites.getSprite(orientation).createSprite());
		}
		RenderUtils.shiftSprites(cannonBarrelSprites, cannonBaseShift);

		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY2, cannonBarrelSprites, entity, protoSelectionBox));
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY2, cannonBaseSprites, entity, protoSelectionBox));
	}

	private List<RotationSprites> getRotatedLayers(LuaValue lua) {
		ArrayList<RotationSprites> ret = new ArrayList<>();
		Utils.forEach(lua, layerLua -> {
			ret.add(getRotatedSprites(layerLua));
		});
		return ret;
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		protoCannonBaseShifts = new ArrayList<>();
		Utils.forEach(prototype.lua().get("cannon_base_shiftings"),
				l -> protoCannonBaseShifts.add(Utils.parsePoint2D(l)));

		LuaValue cannonBaseLayers = prototype.lua().get("cannon_base_pictures").get("layers");
		protoCannonBaseLayers = getRotatedLayers(cannonBaseLayers);

		LuaValue cannonBarrelLayers = prototype.lua().get("cannon_barrel_pictures").get("layers");
		protoCannonBarrelLayers = getRotatedLayers(cannonBarrelLayers);
	}
}
