package com.demod.fbsr.fp;

import java.util.Optional;

import org.luaj.vm2.LuaValue;

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

	@Override
	public Sprite createSprite() {
		return super.createSprite();
	}
}
