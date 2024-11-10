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
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Renderer.Layer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.SpriteDef;
import com.demod.fbsr.WorldMap;

public class ArtilleryTurretRendering extends EntityRendererFactory {

	private Point2D.Double protoBaseShift;
	private List<SpriteDef> protoBaseSprites;
	private List<List<SpriteDef>> protoCannonBarrelSprites;
	private List<List<SpriteDef>> protoCannonBaseSprites;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {

		List<Sprite> baseSprites = RenderUtils.createSprites(protoBaseSprites);
		RenderUtils.shiftSprites(baseSprites, protoBaseShift);
		register.accept(RenderUtils.spriteRenderer(baseSprites, entity, protoSelectionBox));

		List<Sprite> cannonBarrelSprites = RenderUtils.createSprites(protoCannonBarrelSprites, entity.getDirection());
		RenderUtils.shiftSprites(cannonBarrelSprites, protoBaseShift);
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY2, cannonBarrelSprites, entity, protoSelectionBox));

		List<Sprite> cannonBaseSprites = RenderUtils.createSprites(protoCannonBaseSprites, entity.getDirection());
		RenderUtils.shiftSprites(cannonBaseSprites, protoBaseShift);
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY2, cannonBaseSprites, entity, protoSelectionBox));
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		protoBaseShift = Utils.parsePoint2D(prototype.lua().get("base_shift"));
		protoBaseSprites = RenderUtils.getSpritesFromAnimation(prototype.lua().get("base_picture"));
		protoCannonBarrelSprites = new ArrayList<>();
		protoCannonBaseSprites = new ArrayList<>();
		LuaValue cannonBarrel = prototype.lua().get("cannon_barrel_pictures");
		LuaValue cannonBase = prototype.lua().get("cannon_base_pictures");
		for (int i = 0; i < Direction.values().length; i++) {
			int fileNameSelector = i * 2 + 1;
			protoCannonBarrelSprites.add(RenderUtils.getSpritesFromAnimation(cannonBarrel, fileNameSelector));
			protoCannonBaseSprites.add(RenderUtils.getSpritesFromAnimation(cannonBase, fileNameSelector));
		}
	}
}
