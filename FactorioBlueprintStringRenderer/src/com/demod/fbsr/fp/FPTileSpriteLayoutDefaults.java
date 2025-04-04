package com.demod.fbsr.fp;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;

public class FPTileSpriteLayoutDefaults {
	public final Optional<String> spritesheet;
	public final OptionalDouble scale;
	public final OptionalInt x;
	public final OptionalInt y;
	public final OptionalInt tileHeight;
	public final OptionalInt lineLength;
	public final OptionalInt count;

	public FPTileSpriteLayoutDefaults(LuaValue lua, String prefix, boolean ignoreSpritesheet) {
		spritesheet = ignoreSpritesheet ? Optional.empty() : FPUtils.optString(lua.get(prefix + "spritesheet"));
		scale = FPUtils.optDouble(lua.get(prefix + "scale"));
		x = FPUtils.optInt(lua.get(prefix + "x"));
		y = FPUtils.optInt(lua.get(prefix + "y"));
		tileHeight = FPUtils.optInt(lua.get(prefix + "tile_height"));
		lineLength = FPUtils.optInt(lua.get(prefix + "line_length"));
		count = FPUtils.optInt(lua.get(prefix + "count"));
	}

	public FPTileSpriteLayoutDefaults(Optional<String> spritesheet, OptionalDouble scale, OptionalInt x, OptionalInt y,
			OptionalInt tileHeight, OptionalInt lineLength, OptionalInt count) {
		this.spritesheet = spritesheet;
		this.scale = scale;
		this.x = x;
		this.y = y;
		this.tileHeight = tileHeight;
		this.lineLength = lineLength;
		this.count = count;
	}

	public FPTileSpriteLayoutDefaults or(FPTileSpriteLayoutDefaults other) {
		return new FPTileSpriteLayoutDefaults(//
				spritesheet.or(() -> other.spritesheet), //
				scale.isPresent() ? scale : other.scale, //
				x.isPresent() ? x : other.x, //
				y.isPresent() ? y : other.y, //
				tileHeight.isPresent() ? tileHeight : other.tileHeight, //
				lineLength.isPresent() ? lineLength : other.lineLength, //
				count.isPresent() ? count : other.count);
	}
}
