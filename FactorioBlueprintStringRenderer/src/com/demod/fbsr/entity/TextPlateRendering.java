package com.demod.fbsr.entity;

import java.util.function.Consumer;

import org.json.JSONObject;

import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Layer;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.entity.TextPlateRendering.BSTextPlateEntity;
import com.demod.fbsr.fp.FPSpriteVariations;
import com.demod.fbsr.legacy.LegacyBlueprintEntity;

public class TextPlateRendering extends EntityRendererFactory<BSTextPlateEntity> {

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
	public void createRenderers(Consumer<Renderer> register, WorldMap map, BSTextPlateEntity entity) {
		register.accept(RenderUtils.spriteRenderer(protoRenderLayer,
				protoPictures.createSprites(data, entity.variation - 1), entity, protoSelectionBox));
	}

	@Override
	public void initFromPrototype() {
		protoRenderLayer = FPUtils.layer(prototype.lua().get("render_layer"));
		protoPictures = new FPSpriteVariations(prototype.lua().get("pictures"));
	}

}
