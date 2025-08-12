package com.demod.fbsr.entity;

import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.Layer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.WorldMap.BeltBend;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.entity.BSLinkedBeltEntity;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.fp.FPSprite4Way;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapRenderable;

@EntityType("linked-belt")
public class LinkedBeltRendering extends TransportBeltConnectableRendering {

	private FPSprite4Way protoStructureDirectionIn;
	private FPSprite4Way protoStructureDirectionOut;

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);
		
		BSLinkedBeltEntity bsEntity = entity.<BSLinkedBeltEntity>fromBlueprint();

		defineBeltSprites(entity.spriteRegister(register, Layer.TRANSPORT_BELT), entity.getDirection().cardinal(),
				BeltBend.NONE.ordinal(), getAlternatingFrame(entity.getPosition()));

		boolean input = bsEntity.type.orElse("input").equals("input");
		Direction structDir = input ? entity.getDirection() : entity.getDirection().back();
		FPSprite4Way proto = (input ? protoStructureDirectionIn : protoStructureDirectionOut);
		proto.defineSprites(entity.spriteRegister(register, Layer.HIGHER_OBJECT_UNDER), structDir);
	}

	@Override
	public Class<? extends BSEntity> getEntityClass() {
		return BSLinkedBeltEntity.class;
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		super.initAtlas(register);

		protoStructureDirectionIn.getDefs(register);
		protoStructureDirectionOut.getDefs(register);
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		LuaValue luaStructure = prototype.lua().get("structure");
		protoStructureDirectionIn = new FPSprite4Way(profile, luaStructure.get("direction_in"));
		protoStructureDirectionOut = new FPSprite4Way(profile, luaStructure.get("direction_out"));
	}
}
