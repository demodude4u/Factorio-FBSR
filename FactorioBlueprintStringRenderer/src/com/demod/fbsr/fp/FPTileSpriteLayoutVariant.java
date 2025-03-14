package com.demod.fbsr.fp;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.BlendMode;
import com.demod.fbsr.ImageDef;
import com.demod.fbsr.SpriteDef;
import com.demod.fbsr.map.MapRect;

public class FPTileSpriteLayoutVariant {
	public final String spritesheet;
	public final double scale;
	public final int x;
	public final int y;
	public final int tileHeight;
	public final int lineLength;
	public final int count;

	private final int rowCount;

	private final List<SpriteDef> defs;

	public FPTileSpriteLayoutVariant(LuaValue lua, int rowCount) {
		spritesheet = lua.get("spritesheet").tojstring();
		scale = lua.get("scale").optdouble(1.0) * 2;
		x = lua.get("x").optint(0);
		y = lua.get("y").optint(0);
		tileHeight = lua.get("tile_height").optint(1);
		count = lua.get("count").checkint();
		lineLength = lua.get("line_length").optint(count);

		this.rowCount = rowCount;

		defs = createDefs();
	}

	private List<SpriteDef> createDefs() {
		List<SpriteDef> defs = new ArrayList<>();
		int width = (int) Math.round(64 / scale);
		int height = (int) Math.round(tileHeight * 64 / scale);
		Rectangle source = new Rectangle(width, height);
		int spriteCount = rowCount * count;
		for (int i = 0; i < spriteCount; i++) {
			source.x = x + width * (i % lineLength);
			source.y = y + height * (i / lineLength);
			defs.add(new SpriteDef(spritesheet, false, BlendMode.NORMAL, Optional.empty(), false, false, source,
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
