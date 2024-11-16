package com.demod.fbsr.fp;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.FactorioData;
import com.demod.factorio.Utils;
import com.demod.fbsr.FBSR;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Sprite;

public class FPSpriteParameters extends FPSpriteSource {
	public final String blendMode;
	public final boolean drawAsShadow;
	public final double scale;
	public final FPVector shift;
	public final FPColor tint;

	public FPSpriteParameters(LuaValue lua) {
		super(lua);

		blendMode = lua.get("blend_mode").optjstring("normal");
		drawAsShadow = lua.get("draw_as_shadow").optboolean(false);
		scale = lua.get("scale").optdouble(1);
		shift = FPVector.opt(lua.get("shift"), 0, 0);
		tint = FPColor.opt(lua, 1, 1, 1, 1);
	}

	protected Sprite createSprite() {
		Sprite ret = new Sprite();
		ret.image = FactorioData.getModImage(filename.get());
		ret.shadow = drawAsShadow;

		if (!blendMode.equals("normal")) { // FIXME blending will take effort
			ret.image = RenderUtils.EMPTY_IMAGE;
		}

		Color tintColor = tint.createColorIgnorePreMultipliedAlpha();
		if (!tintColor.equals(Color.white)) {
			ret.image = Utils.tintImage(ret.image, tintColor);
		}

		double scaledWidth = scale * width / FBSR.tileSize;
		double scaledHeight = scale * height / FBSR.tileSize;
		ret.source = new Rectangle(x, y, width, height);
		ret.bounds = new Rectangle2D.Double(shift.x - scaledWidth / 2.0, shift.y - scaledHeight / 2.0, scaledWidth,
				scaledHeight);
		return ret;
	}
}
