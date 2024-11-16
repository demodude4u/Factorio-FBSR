package com.demod.fbsr.entity;

import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Renderer.Layer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.fp.FPAnimation4Way;
import com.demod.fbsr.fp.FPRotatedSprite;
import com.demod.fbsr.fp.FPVector3D;

public class ArtilleryTurretRendering extends EntityRendererFactory {

	private FPVector3D protoCannonBaseShift;
	private FPAnimation4Way protoBaseSprites;
	private FPRotatedSprite protoCannonBaseSprites;
	private FPRotatedSprite protoCannonBarrelSprites;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {

		// TODO cannon base shift, in 3D?

		register.accept(RenderUtils.spriteRenderer(protoBaseSprites.createSprites(entity.getDirection(), 0), entity,
				protoSelectionBox));
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY2,
				protoCannonBaseSprites.createSprites(entity.getDirection().getOrientation()), entity,
				protoSelectionBox));
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY2,
				protoCannonBarrelSprites.createSprites(entity.getDirection().getOrientation()), entity,
				protoSelectionBox));
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		protoCannonBaseShift = new FPVector3D(prototype.lua().get("cannon_base_shift"));
		protoBaseSprites = new FPAnimation4Way(prototype.lua().get("base_picture"));
		protoCannonBaseSprites = new FPRotatedSprite(prototype.lua().get("cannon_base_pictures"));
		protoCannonBarrelSprites = new FPRotatedSprite(prototype.lua().get("cannon_barrel_pictures"));
	}
}
