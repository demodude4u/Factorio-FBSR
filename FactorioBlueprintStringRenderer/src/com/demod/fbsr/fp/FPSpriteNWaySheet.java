package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.SpriteDef;

public class FPSpriteNWaySheet extends FPSpriteParameters {

	public static Optional<FPSpriteNWaySheet> opt(LuaValue lua, int directionCount) {
		if (lua.isnil()) {
			return Optional.empty();
		}
		return Optional.of(new FPSpriteNWaySheet(lua, directionCount));
	}

	public final int frames;

	private final int directionCount;

	private final List<SpriteDef> defs;

	public FPSpriteNWaySheet(LuaValue lua, int directionCount) {
		super(lua);
		this.directionCount = directionCount;

		frames = lua.get("frames").optint(directionCount);

		defs = createDefs();
	}

	private List<SpriteDef> createDefs() {
		List<SpriteDef> defs = new ArrayList<>();
		for (Direction direction : Direction.values()) {
			int x = this.x;

			if (directionCount == 4) {
				x += width * (direction.cardinal() % frames);
			} else if (directionCount == 8) {
				x += width * (direction.ordinal() % frames);
			}

			defs.add(SpriteDef.fromFP(filename.get(), drawAsShadow, blendMode, tint, x, y, width, height, shift.x,
					shift.y, scale));
		}
		return defs;
	}

	public SpriteDef defineSprite(Direction direction) {
		return defs.get(direction.ordinal());
	}
}
