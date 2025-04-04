package com.demod.fbsr.fp;

import java.util.List;
import java.util.Optional;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;

public class FPTileTransitionsVariants {
	public final List<FPTileMainPictures> main;
	public final int materialTextureWidthInTiles;
	public final int materialTextureHeightInTiles;
	public final Optional<FPMaterialTextureParameters> materialBackground;
	public final List<FPTileLightPictures> light;
	public final Optional<FPMaterialTextureParameters> materialLight;
	public final boolean emptyTransitions;
	public final Optional<FPTileTransitions> transition;

	public FPTileTransitionsVariants(LuaValue lua, int limitCount) {
		main = FPUtils.list(lua.get("main"), l -> new FPTileMainPictures(l, limitCount));
		materialTextureWidthInTiles = lua.get("material_texture_width_in_tiles").optint(8);
		materialTextureHeightInTiles = lua.get("material_texture_height_in_tiles").optint(8);
		materialBackground = FPUtils.opt(lua.get("material_background"), l -> new FPMaterialTextureParameters(l,
				materialTextureWidthInTiles, materialTextureHeightInTiles, limitCount));
		light = FPUtils.list(lua.get("light"), FPTileLightPictures::new);
		materialLight = FPUtils.opt(lua.get("material_light"), l -> new FPMaterialTextureParameters(l,
				materialTextureWidthInTiles, materialTextureHeightInTiles, limitCount));
		emptyTransitions = lua.get("empty_transitions").optboolean(false);
		transition = FPUtils.opt(lua.get("transition"), FPTileTransitions::new);
	}
}
