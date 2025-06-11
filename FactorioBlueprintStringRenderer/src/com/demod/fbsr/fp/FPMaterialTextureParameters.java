package com.demod.fbsr.fp;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Profile;
import com.demod.fbsr.def.MaterialDef;

public class FPMaterialTextureParameters {
	private final int texWidthTiles;
	private final int texHeightTiles;

	public final int count;
	public final String picture;
	public final double scale;
	public final int x;
	public final int y;
	public final int lineLength;

	private final int limitedCount;
	private final List<MaterialDef> defs;

	public FPMaterialTextureParameters(Profile profile, LuaValue lua, int texWidthTiles, int texHeightTiles, int limitCount) {
		this.texWidthTiles = texWidthTiles;
		this.texHeightTiles = texHeightTiles;

		count = lua.get("count").checkint();
		picture = lua.get("picture").checkjstring();
		scale = lua.get("scale").optdouble(1.0) * 2;
		x = lua.get("x").optint(0);
		y = lua.get("y").optint(0);
		lineLength = lua.get("line_length").optint(count);

		limitedCount = Math.min(limitCount, count);
		List<MaterialDef> allDefs = createDefs(profile);
		defs = allDefs.stream().limit(limitedCount).collect(Collectors.toList());
	}

	private List<MaterialDef> createDefs(Profile profile) {
		List<MaterialDef> defs = new ArrayList<>();

		int width = (int) (texWidthTiles * 64 / scale);
		int height = (int) (texHeightTiles * 64 / scale);

		for (int i = 0; i < count; i++) {
			int x = width * (i % lineLength);
			int y = height * (i / lineLength);
			defs.add(new MaterialDef(profile, picture, new Rectangle(x, y, width, height), texHeightTiles, texWidthTiles));
		}

		return defs;
	}

	public MaterialDef defineMaterial(int frame) {
		return defs.get(frame);
	}

	public List<MaterialDef> getDefs() {
		return defs;
	}

	public int getTexHeightTiles() {
		return texHeightTiles;
	}

	public int getTexWidthTiles() {
		return texWidthTiles;
	}

	public int getLimitedCount() {
		return limitedCount;
	}
}
