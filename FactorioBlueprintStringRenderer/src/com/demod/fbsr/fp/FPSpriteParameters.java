package com.demod.fbsr.fp;

import java.util.Optional;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.BlendMode;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.SpriteDef;

public class FPSpriteParameters extends FPSpriteSource {
	public final BlendMode blendMode;
	public final boolean drawAsShadow;
	public final double scale;
	public final FPVector shift;
	public final Optional<FPColor> tint;
	public final boolean tintAsOverlay;
	public final boolean applyRuntimeTint;

	private final SpriteDef def;

	public FPSpriteParameters(LuaValue lua) {
		super(lua);

		blendMode = FPUtils.blendMode(lua.get("blend_mode"));
		drawAsShadow = lua.get("draw_as_shadow").optboolean(false);
		scale = lua.get("scale").optdouble(1) * 2;
		shift = FPUtils.opt(lua.get("shift"), FPVector::new).orElseGet(() -> new FPVector(0, 0));
		tint = FPUtils.tint(lua.get("tint"));
		tintAsOverlay = lua.get("tint_as_overlay").optboolean(false);
		applyRuntimeTint = lua.get("apply_runtime_tint").optboolean(false);

		if (filename.isPresent()) {
			def = SpriteDef.fromFP(filename.get(), drawAsShadow, blendMode, tint, tintAsOverlay, applyRuntimeTint, x, y,
					width, height, shift.x, shift.y, scale);
		} else {
			def = null;
		}
	}

	protected SpriteDef defineSprite() {
		return def;
	}
}
