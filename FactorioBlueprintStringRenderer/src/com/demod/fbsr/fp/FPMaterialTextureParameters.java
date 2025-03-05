package com.demod.fbsr.fp;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.ImageDef;

public class FPMaterialTextureParameters {
	private final int texWidthTiles;
	private final int texHeightTiles;

	public final int count;
	public final String picture;
	public final double scale;
	public final int x;
	public final int y;
	public final int lineLength;

	private final List<ImageDef> defs;

	public FPMaterialTextureParameters(LuaValue lua, int texWidthTiles, int texHeightTiles) {
		this.texWidthTiles = texWidthTiles;
		this.texHeightTiles = texHeightTiles;

		count = lua.get("count").checkint();
		picture = lua.get("picture").checkjstring();
		scale = lua.get("scale").optdouble(1.0) * 2;
		x = lua.get("x").optint(0);
		y = lua.get("y").optint(0);
		lineLength = lua.get("line_length").optint(count);

		defs = createDefs();
	}

	private List<ImageDef> createDefs() {
		List<ImageDef> defs = new ArrayList<>();

		int width = (int) (texWidthTiles * 64 / scale);
		int height = (int) (texHeightTiles * 64 / scale);

		for (int i = 0; i < count; i++) {
			int x = width * (i % lineLength);
			int y = height * (i / lineLength);
			defs.add(new ImageDef(picture, new Rectangle(x, y, width, height)));
		}

		return defs;
	}

	public ImageDef defineImage(int frame) {
		return defs.get(frame);
	}

	public List<ImageDef> getDefs() {
		return defs;
	}

	public int getTexHeightTiles() {
		return texHeightTiles;
	}

	public int getTexWidthTiles() {
		return texWidthTiles;
	}
}
