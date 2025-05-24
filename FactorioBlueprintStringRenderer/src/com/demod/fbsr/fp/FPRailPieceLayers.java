package com.demod.fbsr.fp;

import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.ModsProfile;
import com.demod.fbsr.def.ImageDef;

public class FPRailPieceLayers {
	public final Optional<FPSpriteVariations> stonePathBackground;
	public final Optional<FPSpriteVariations> stonePath;
	public final Optional<FPSpriteVariations> ties;
	public final Optional<FPSpriteVariations> backplates;
	public final Optional<FPSpriteVariations> metals;

	public FPRailPieceLayers(ModsProfile profile, LuaValue lua) {
		stonePathBackground = FPUtils.opt(profile, lua.get("stone_path_background"), FPSpriteVariations::new);
		stonePath = FPUtils.opt(profile, lua.get("stone_path"), FPSpriteVariations::new);
		ties = FPUtils.opt(profile, lua.get("ties"), FPSpriteVariations::new);
		backplates = FPUtils.opt(profile, lua.get("backplates"), FPSpriteVariations::new);
		metals = FPUtils.opt(profile, lua.get("metals"), FPSpriteVariations::new);
	}

	public void getDefs(Consumer<ImageDef> register, int variation) {
		stonePathBackground.ifPresent(fp -> fp.defineSprites(register, variation));
		stonePath.ifPresent(fp -> fp.defineSprites(register, variation));
		ties.ifPresent(fp -> fp.defineSprites(register, variation));
		backplates.ifPresent(fp -> fp.defineSprites(register, variation));
		metals.ifPresent(fp -> fp.defineSprites(register, variation));
	}
}