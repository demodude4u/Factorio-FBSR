package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.def.SpriteDef;
import com.demod.fbsr.Dir16;

public class FPSpriteNWaySheet extends FPSpriteParameters {

	public static Optional<FPSpriteNWaySheet> opt(LuaValue lua, int directionCount) {
		if (lua.isnil()) {
			return Optional.empty();
		}
		return Optional.of(new FPSpriteNWaySheet(lua, directionCount));
	}

	public final int frames;
	private int frameRepeat;

	private final int directionCount;

	private final List<SpriteDef> defs;


	public FPSpriteNWaySheet(LuaValue lua, int directionCount) {
		super(lua);
		this.directionCount = directionCount;

		frames = lua.get("frames").optint(directionCount);
		frameRepeat  = lua.get("frame_repeat").optint(1);

		defs = createDefs();
	}

	private List<SpriteDef> createDefs() {
		List<SpriteDef> defs = new ArrayList<>();
		for (Dir16 direction : Dir16.values()) {
			int x = this.x;

			if (directionCount == 4) {
				x += width * ((direction.ordinal() / (4 * frameRepeat)) % frames);
			} else if (directionCount == 8) {
				x += width * ((direction.ordinal() / (2 * frameRepeat)) % frames);
			} else if (directionCount == 16) {
				x += width * ((direction.ordinal() / (frameRepeat)) % frames);
			}

			defs.add(SpriteDef.fromFP(filename.get(), drawAsShadow, blendMode, tint, tintAsOverlay, applyRuntimeTint, x,
					y, width, height, shift.x, shift.y, scale));
		}
		return defs;
	}

	public SpriteDef defineSprite(Direction direction) {
		return defs.get(direction.ordinal());
	}

	public SpriteDef defineSprite(Dir16 direction) {
		return defs.get(direction.ordinal());
	}
}
