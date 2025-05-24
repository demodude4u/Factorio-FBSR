package com.demod.fbsr.fp;

import java.util.List;
import java.util.Optional;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.ModsProfile;

public class FPTileTransitionsVariants {
	public final List<FPTileMainPictures> main;
	public final int materialTextureWidthInTiles;
	public final int materialTextureHeightInTiles;
	public final Optional<FPMaterialTextureParameters> materialBackground;
	public final List<FPTileLightPictures> light;
	public final Optional<FPMaterialTextureParameters> materialLight;
	public final boolean emptyTransitions;
	public final Optional<FPTileTransitions> transition;

	public FPTileTransitionsVariants(ModsProfile profile, LuaValue lua, int limitCount) {
		main = FPUtils.list(lua.get("main"), l -> new FPTileMainPictures(profile, l, limitCount));
		materialTextureWidthInTiles = lua.get("material_texture_width_in_tiles").optint(8);
		materialTextureHeightInTiles = lua.get("material_texture_height_in_tiles").optint(8);
		materialBackground = FPUtils.opt(profile, lua.get("material_background"), (p, l) -> new FPMaterialTextureParameters(p, l,
				materialTextureWidthInTiles, materialTextureHeightInTiles, limitCount));
		light = FPUtils.list(profile, lua.get("light"), FPTileLightPictures::new);
		materialLight = FPUtils.opt(profile, lua.get("material_light"), (p, l) -> new FPMaterialTextureParameters(p, l,
				materialTextureWidthInTiles, materialTextureHeightInTiles, limitCount));
		emptyTransitions = lua.get("empty_transitions").optboolean(false);
		transition = FPUtils.opt(profile, lua.get("transition"), FPTileTransitions::new);
	}
}
