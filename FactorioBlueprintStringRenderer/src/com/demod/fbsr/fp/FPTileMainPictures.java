package com.demod.fbsr.fp;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.BlendMode;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Profile;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.SpriteDef;
import com.demod.fbsr.map.MapRect;

public class FPTileMainPictures extends FPTileSpriteLayout {
	public final int size;
	public final double probability;
	public final Optional<List<Double>> weights;

	private final int limitedCount;
	private final List<SpriteDef> defs;

	public FPTileMainPictures(Profile profile, LuaValue lua, int limitCount) {
		super(lua);
		size = lua.get("size").checkint();
		probability = lua.get("probability").optdouble(1.0);
		weights = FPUtils.optList(lua.get("weights"), LuaValue::todouble);

		limitedCount = Math.min(limitCount, count);
		List<SpriteDef> allDefs = createDefs(profile);
		defs = allDefs.stream().limit(limitedCount).collect(Collectors.toList());
		FPUtils.verifyNotNull(lua.getDebugPath() + " defs", defs);
	}

	private List<SpriteDef> createDefs(Profile profile) {
		List<SpriteDef> defs = new ArrayList<>();

		int sizePixels = (int) (size * 64 / scale);

		for (int i = 0; i < count; i++) {
			int x = sizePixels * (i % lineLength);
			int y = sizePixels * (i / lineLength);
			defs.add(new SpriteDef(profile, picture, false, BlendMode.NORMAL, Optional.empty(), false, false,
					new Rectangle(x, y, sizePixels, sizePixels), MapRect.byUnit(0, 0, size, size)));
		}

		return defs;
	}

	public SpriteDef defineImage(int frame) {
		return defs.get(frame);
	}

	public void getDefs(Consumer<ImageDef> register) {
		defs.forEach(register);
	}

	public int getLimitedCount() {
		return limitedCount;
	}
}