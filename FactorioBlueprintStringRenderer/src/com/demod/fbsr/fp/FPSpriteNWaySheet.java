package com.demod.fbsr.fp;

import java.util.Optional;

import org.luaj.vm2.LuaValue;

import com.demod.fbsr.Direction;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Sprite;

public class FPSpriteNWaySheet extends FPSpriteParameters {

	public static Optional<FPSpriteNWaySheet> opt(LuaValue lua, int defaultFrames) {
		if (lua.isnil()) {
			return Optional.empty();
		}
		return Optional.of(new FPSpriteNWaySheet(lua, defaultFrames));
	}

	public final int frames;

	public FPSpriteNWaySheet(LuaValue lua, int defaultFrames) {
		super(lua);

		frames = lua.get("frames").optint(defaultFrames);
	}

	public Sprite createSprite(Direction direction) {
		int x = this.x;

		// TODO what do I do when it is not 4 or 8?
		if (frames == 4) {
			x += width * direction.cardinal();
		} else if (frames == 8) {
			x += width * direction.ordinal();
		}

		return RenderUtils.createSprite(filename.get(), drawAsShadow, blendMode, getEffectiveTint(), x, y, width,
				height, shift.x, shift.y, scale);
	}
}
