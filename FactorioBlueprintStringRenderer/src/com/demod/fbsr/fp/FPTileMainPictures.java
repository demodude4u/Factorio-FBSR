package com.demod.fbsr.fp;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.ImageDef;

public class FPTileMainPictures extends FPTileSpriteLayout {
	public final int size;
	public final double probability;
	public final Optional<List<Double>> weights;

	private final int limitedCount;
	private final List<ImageDef> defs;

	public FPTileMainPictures(LuaValue lua, int limitCount) {
		super(lua);
		size = lua.get("size").checkint();
		probability = lua.get("probability").optdouble(1.0);
		weights = FPUtils.optList(lua.get("weights"), LuaValue::todouble);

		limitedCount = Math.min(limitCount, count);
		List<ImageDef> allDefs = createDefs();
		defs = allDefs.stream().limit(limitedCount).collect(Collectors.toList());
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

	public List<ImageDef> getDefs() {
		return defs;
	}

	public int getLimitedCount() {
		return limitedCount;
	}
}