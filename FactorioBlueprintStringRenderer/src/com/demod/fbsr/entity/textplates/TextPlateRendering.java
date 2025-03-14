package com.demod.fbsr.entity.textplates;

import java.util.function.Consumer;

import org.json.JSONObject;

import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Layer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.SpriteDef;
import com.demod.fbsr.fp.FPSpriteVariations;
import com.demod.fbsr.legacy.LegacyBlueprintEntity;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapRenderable;

public class TextPlateRendering extends EntityRendererFactory {

	public static class BSTextPlateEntity extends BSEntity {

		private final int variation;

		public BSTextPlateEntity(JSONObject json) {
			super(json);

			variation = json.optInt("variation");
		}

		public BSTextPlateEntity(LegacyBlueprintEntity legacy) {
			super(legacy);

			variation = legacy.json().optInt("variation");
		}
	}

	private Layer protoRenderLayer;
	private FPSpriteVariations protoPictures;

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		Consumer<SpriteDef> spriteRegister = entity.spriteRegister(register, protoRenderLayer);
		protoPictures.defineSprites(spriteRegister, entity.<BSTextPlateEntity>fromBlueprint().variation - 1);
	}

	@Override
	public void initFromPrototype() {
		protoRenderLayer = FPUtils.layer(prototype.lua().get("render_layer"));
		protoPictures = new FPSpriteVariations(prototype.lua().get("pictures"));
	}

	@Override
	public Class<? extends BSEntity> getEntityClass() {
		return BSTextPlateEntity.class;
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		protoPictures.getDefs(register);
	}

}
