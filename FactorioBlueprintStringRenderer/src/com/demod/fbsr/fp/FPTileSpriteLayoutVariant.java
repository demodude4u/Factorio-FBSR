package com.demod.fbsr.fp;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.ImageDef;

public class FPTileSpriteLayoutVariant {
	public final String spritesheet;
	public final double scale;
	public final int x;
	public final int y;
	public final int tileHeight;
	public final int lineLength;
	public final int count;

	private final List<ImageDef> defs;

	public FPTileSpriteLayoutVariant(LuaValue lua) {
		spritesheet = lua.get("spritesheet").tojstring();
		scale = lua.get("scale").optdouble(1.0) * 2;
		x = lua.get("x").optint(0);
		y = lua.get("y").optint(0);
		tileHeight = lua.get("tile_height").optint(1);
		count = lua.get("count").checkint();
		lineLength = lua.get("line_length").optint(count);

		defs = createDefs();
	}

	private List<ImageDef> createDefs() {
		List<ImageDef> defs = new ArrayList<>();
		int width = (int) Math.round(64 / scale);
		int height = (int) Math.round(tileHeight * 64 / scale);
		Rectangle source = new Rectangle(width, height);
		for (int i = 0; i < count; i++) {
			source.x = x + width * (i % lineLength);
			source.y = y + height * (i / lineLength);
			defs.add(new ImageDef(spritesheet, source));
		}
		return defs;
	}

	public ImageDef defineImage(int variant) {
		return defs.get(variant);
	}

	public List<ImageDef> getDefs() {
		return defs;
	}
}
