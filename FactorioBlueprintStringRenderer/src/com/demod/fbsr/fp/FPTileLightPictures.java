package com.demod.fbsr.fp;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.ImageDef;

public class FPTileLightPictures extends FPTileSpriteLayout {
	public final int size;

	private final List<ImageDef> defs;

	public FPTileLightPictures(LuaValue lua) {
		super(lua);
		size = lua.get("size").checkint();

		defs = createDefs();
	}

	private List<ImageDef> createDefs() {
		List<ImageDef> defs = new ArrayList<>();

		int sizePixels = (int) (size * 64 / scale);

		for (int i = 0; i < count; i++) {
			int x = sizePixels * (i % lineLength);
			int y = sizePixels * (i / lineLength);
			defs.add(new ImageDef(picture, new Rectangle(x, y, sizePixels, sizePixels)));
		}

		return defs;
	}

	public ImageDef defineImage(int frame) {
		return defs.get(frame);
	}
}