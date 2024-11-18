package com.demod.fbsr.fp;

import java.awt.Color;
import java.util.Optional;

import org.luaj.vm2.LuaValue;

import com.demod.fbsr.FPUtils;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Sprite;

public class FPSpriteParameters extends FPSpriteSource {
	public final String blendMode;
	public final boolean drawAsShadow;
	public final double scale;
	public final FPVector shift;
	public final FPColor tint;
	public final boolean applyRuntimeTint;

	// XXX hacky, violates immutability
	public Optional<Color> runtimeTint = Optional.empty();

	public FPSpriteParameters(LuaValue lua) {
		super(lua);

		blendMode = lua.get("blend_mode").optjstring("normal");
		drawAsShadow = lua.get("draw_as_shadow").optboolean(false);
		scale = lua.get("scale").optdouble(1);
		shift = FPUtils.opt(lua.get("shift"), FPVector::new).orElseGet(() -> new FPVector(0, 0));
		tint = FPUtils.opt(lua, FPColor::new).orElseGet(() -> new FPColor(1, 1, 1, 1));
		applyRuntimeTint = lua.get("apply_runtime_tint").optboolean(false);
	}

	protected Sprite createSprite() {

		return RenderUtils.createSprite(filename.get(), drawAsShadow, blendMode, getEffectiveTint(), x, y, width,
				height, shift.x, shift.y, scale);
	}

	public Color getEffectiveTint() {
		Color tint = this.tint.createColorIgnorePreMultipliedAlpha();
		if (applyRuntimeTint && runtimeTint.isPresent()) {
			tint = runtimeTint.get();
		}
		return tint;
	}

	public void setRuntimeTint(Color tint) {
		runtimeTint = Optional.of(tint);
	}
}
