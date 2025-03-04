package com.demod.fbsr.entity;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.swing.Renderer;

import org.json.JSONObject;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.BSUtils;
import com.demod.fbsr.Direction;
import com.demod.fbsr.Layer;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.WorldMap.BeltBend;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.entity.LinkedBeltRendering.BSLinkedBeltEntity;
import com.demod.fbsr.fp.FPSprite4Way;
import com.demod.fbsr.legacy.LegacyBlueprintEntity;

public class LinkedBeltRendering extends TransportBeltConnectableRendering<BSLinkedBeltEntity> {

	public static class BSLinkedBeltEntity extends BSEntity {
		public final Optional<String> type;

		public BSLinkedBeltEntity(JSONObject json) {
			super(json);

			type = BSUtils.optString(json, "type");
		}

		public BSLinkedBeltEntity(LegacyBlueprintEntity legacy) {
			super(legacy);

			type = BSUtils.optString(legacy.json(), "type");
		}

	}

	private FPSprite4Way protoStructureDirectionIn;
	private FPSprite4Way protoStructureDirectionOut;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, BSLinkedBeltEntity entity) {
		List<Sprite> beltSprites = createBeltSprites(entity.direction.cardinal(), BeltBend.NONE.ordinal(),
				getAlternatingFrame(entity.position.createPoint(), 0));
		register.accept(RenderUtils.spriteRenderer(Layer.TRANSPORT_BELT, beltSprites, entity, drawBounds));

		boolean input = entity.type.get().equals("input");
		Direction structDir = input ? entity.direction : entity.direction.back();
		List<Sprite> structureSprites = (input ? protoStructureDirectionIn : protoStructureDirectionOut)
				.createSprites(data, structDir);
		register.accept(
				RenderUtils.spriteRenderer(Layer.HIGHER_OBJECT_UNDER, structureSprites, entity, drawBounds));
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		LuaValue luaStructure = prototype.lua().get("structure");
		protoStructureDirectionIn = new FPSprite4Way(luaStructure.get("direction_in"));
		protoStructureDirectionOut = new FPSprite4Way(luaStructure.get("direction_out"));
	}
}
