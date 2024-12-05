package com.demod.fbsr.entity;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.json.JSONObject;
import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.BSUtils;
import com.demod.fbsr.Direction;
import com.demod.fbsr.Layer;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.WorldMap.BeltBend;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.entity.LinkedBeltRendering.BSLinkedBeltEntity;
import com.demod.fbsr.fp.FPSprite4Way;

public class LinkedBeltRendering extends TransportBeltConnectableRendering<BSLinkedBeltEntity> {

	public static class BSLinkedBeltEntity extends BSEntity {
		public final Optional<String> type;

		public BSLinkedBeltEntity(JSONObject json) {
			super(json);

			type = BSUtils.optString(json, "type");
		}
	}

	private FPSprite4Way protoStructureDirectionIn;
	private FPSprite4Way protoStructureDirectionOut;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BSLinkedBeltEntity entity) {
		List<Sprite> beltSprites = createBeltSprites(entity.direction.cardinal(), BeltBend.NONE.ordinal(),
				getAlternatingFrame(entity.position.createPoint(), 0));
		register.accept(RenderUtils.spriteRenderer(Layer.TRANSPORT_BELT, beltSprites, entity, protoSelectionBox));

		boolean input = entity.type.get().equals("input");
		Direction structDir = input ? entity.direction : entity.direction.back();
		List<Sprite> structureSprites = (input ? protoStructureDirectionIn : protoStructureDirectionOut)
				.createSprites(structDir);
		register.accept(
				RenderUtils.spriteRenderer(Layer.HIGHER_OBJECT_UNDER, structureSprites, entity, protoSelectionBox));
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		LuaValue luaStructure = prototype.lua().get("structure");
		protoStructureDirectionIn = new FPSprite4Way(luaStructure.get("direction_in"));
		protoStructureDirectionOut = new FPSprite4Way(luaStructure.get("direction_out"));
	}
}
