package com.demod.fbsr.fp;

import java.util.Optional;

import org.luaj.vm2.LuaValue;

import com.demod.fbsr.Direction;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Sprite;

public class FPSpriteNWaySheet extends FPSpriteParameters {

	public static Optional<FPSpriteNWaySheet> opt(LuaValue lua, int directionCount) {
		if (lua.isnil()) {
			return Optional.empty();
		}
		return Optional.of(new FPSpriteNWaySheet(lua, directionCount));
	}

	public final int frames;

	private final int directionCount;

	public FPSpriteNWaySheet(LuaValue lua, int directionCount) {
		super(lua);
		this.directionCount = directionCount;

		frames = lua.get("frames").optint(directionCount);
	}

	public Sprite createSprite(Direction direction) {
		int x = this.x;

		if (directionCount == 4) {
			x += width * (direction.cardinal() % frames);
		} else if (directionCount == 8) {
			x += width * (direction.ordinal() % frames);
		}

		return RenderUtils.createSprite(filename.get(), drawAsShadow, blendMode, getEffectiveTint(), x, y, width,
				height, shift.x, shift.y, scale);
	}
}
