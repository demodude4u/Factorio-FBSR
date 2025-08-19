package com.demod.fbsr.fp;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.BlendMode;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Profile;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.SpriteDef;
import com.demod.fbsr.map.MapRect;
import com.google.common.collect.ImmutableList;

public class FPTileSpriteLayoutVariant {
	public final Optional<String> spritesheet;
	public final double scale;
	public final int x;
	public final int y;
	public final int tileHeight;
	public final int count;
	public final OptionalInt lineLength;

	private final OptionalInt rowCount;

	private final List<SpriteDef> defs;

	public FPTileSpriteLayoutVariant(Profile profile, LuaValue lua, Optional<String> overrideSpritesheet,
			FPTileSpriteLayoutDefaults defaults, OptionalInt rowCount) {
		this.spritesheet = overrideSpritesheet.or(() -> FPUtils.optString(lua.get("spritesheet")))
				.or(() -> defaults.spritesheet);
		scale = FPUtils.optDouble(lua.get("scale")).orElse(defaults.scale.orElse(2.0) / 2.0) * 2.0;
		x = FPUtils.optInt(lua.get("x")).orElse(defaults.x.orElse(0));
		y = FPUtils.optInt(lua.get("y")).orElse(defaults.y.orElse(0));
		tileHeight = FPUtils.optInt(lua.get("tile_height")).orElse(defaults.tileHeight.orElse(1));
		count = FPUtils.optInt(lua.get("count")).orElseGet(() -> defaults.count.getAsInt());
		lineLength = FPUtils.optInt(lua.get("line_length"), defaults.lineLength.orElse(count));

		this.rowCount = rowCount;

		defs = createDefs(profile);
		FPUtils.verifyNotNull(lua.getDebugPath() + " defs", defs);
	}

	private List<SpriteDef> createDefs(Profile profile) {
		if (rowCount.isEmpty()) {
			return ImmutableList.of();
		}

		if (spritesheet.isEmpty()) {
			return ImmutableList.of();
		}

		List<SpriteDef> defs = new ArrayList<>();
		int width = (int) Math.round(64 / scale);
		int height = (int) Math.round(tileHeight * 64 / scale);
		Rectangle source = new Rectangle(width, height);
		int spriteCount = rowCount.getAsInt() * count;
		int lineLength = this.lineLength.orElse(count);
		for (int i = 0; i < spriteCount; i++) {
			source.x = x + width * (i % lineLength);
			source.y = y + height * (i / lineLength);
			defs.add(new SpriteDef(profile, spritesheet.get(), false, BlendMode.NORMAL, Optional.empty(), false, false, source,
					MapRect.byUnit(0, 0, 1, tileHeight)));
		}
		return defs;
	}

	public SpriteDef defineImage(int row, int col) {
		return defs.get(row * count + col);
	}

	public void getDefs(Consumer<ImageDef> register) {
		defs.forEach(register);
	}
}
