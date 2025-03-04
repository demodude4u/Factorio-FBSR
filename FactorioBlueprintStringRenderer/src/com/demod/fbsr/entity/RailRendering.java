package com.demod.fbsr.entity;

import java.util.function.Consumer;

import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.ImageDef;
import com.demod.fbsr.Layer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.fp.FPRailPictureSet;
import com.demod.fbsr.fp.FPRailPieceLayers;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapRect3D;
import com.demod.fbsr.map.MapRenderable;

public abstract class RailRendering extends EntityRendererFactory {
	private static final int VARIATION = 0;
	// TODO add variation (or should we ignore variations to save atlas space?)

	private final boolean elevated;

	protected FPRailPictureSet protoPictures;
	private final Layer layerRailStoneBackground;
	private final Layer layerRailStone;
	private final Layer layerRailTies;
	private final Layer layerRailBackplates;
	private final Layer layerRailMetals;

	public RailRendering(boolean elevated) {
		this.elevated = elevated;
		if (elevated) {
			layerRailStoneBackground = Layer.ELEVATED_RAIL_STONE_PATH_LOWER;
			layerRailStone = Layer.ELEVATED_RAIL_STONE_PATH;
			layerRailTies = Layer.ELEVATED_RAIL_TIE;
			layerRailBackplates = Layer.ELEVATED_RAIL_SCREW;
			layerRailMetals = Layer.ELEVATED_RAIL_METAL;
		} else {
			layerRailStoneBackground = Layer.RAIL_STONE_PATH_LOWER;
			layerRailStone = Layer.RAIL_STONE_PATH;
			layerRailTies = Layer.RAIL_TIE;
			layerRailBackplates = Layer.RAIL_SCREW;
			layerRailMetals = Layer.RAIL_METAL;
		}
	}

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		FPRailPieceLayers railPieceLayers = protoPictures.get(entity.getDirection());
		if (railPieceLayers.stonePathBackground.isPresent()) {
			railPieceLayers.stonePathBackground.get()
					.defineSprites(entity.spriteRegister(register, layerRailStoneBackground), VARIATION);
		}
		if (railPieceLayers.stonePath.isPresent()) {
			railPieceLayers.stonePath.get().defineSprites(entity.spriteRegister(register, layerRailStone), VARIATION);
		}
		if (railPieceLayers.ties.isPresent()) {
			railPieceLayers.ties.get().defineSprites(entity.spriteRegister(register, layerRailTies), VARIATION);
		}
		if (railPieceLayers.backplates.isPresent()) {
			railPieceLayers.backplates.get().defineSprites(entity.spriteRegister(register, layerRailBackplates),
					VARIATION);
		}
		if (railPieceLayers.metals.isPresent()) {
			railPieceLayers.metals.get().defineSprites(entity.spriteRegister(register, layerRailMetals), VARIATION);
		}
	}

	@Override
	public void initFromPrototype() {
		protoPictures = new FPRailPictureSet(prototype.lua().get("pictures"));
	}

	@Override
	protected MapRect3D computeBounds() {
		MapRect3D ret = super.computeBounds();
		if (elevated) {
			ret = ret.shiftHeightUnit(3);
		}
		return ret;
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		protoPictures.getDefs(register, VARIATION);
	}

}
