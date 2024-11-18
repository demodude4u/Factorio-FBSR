package com.demod.fbsr.entity;

import java.util.List;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.Direction;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Renderer.Layer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.WorldMap.BeltBend;
import com.demod.fbsr.fp.FPSprite4Way;

public class LinkedBeltRendering extends TransportBeltConnectableRendering {

	private FPSprite4Way protoStructureDirectionIn;
	private FPSprite4Way protoStructureDirectionOut;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BlueprintEntity entity) {
		List<Sprite> beltSprites = createBeltSprites(entity.getDirection().cardinal(), BeltBend.NONE.ordinal(), 0);
		register.accept(RenderUtils.spriteRenderer(beltSprites, entity, protoSelectionBox));

		boolean input = entity.json().getString("type").equals("input");
		Direction structDir = input ? entity.getDirection() : entity.getDirection().back();
		List<Sprite> structureSprites = (input ? protoStructureDirectionIn : protoStructureDirectionOut)
				.createSprites(structDir);
		register.accept(RenderUtils.spriteRenderer(Layer.ENTITY2, structureSprites, entity, protoSelectionBox));
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		LuaValue luaStructure = prototype.lua().get("structure");
		protoStructureDirectionIn = new FPSprite4Way(luaStructure.get("direction_in"));
		protoStructureDirectionOut = new FPSprite4Way(luaStructure.get("direction_out"));
	}
}
