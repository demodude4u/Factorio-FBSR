package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Sprite;

public class FPAnimation extends FPAnimationParameters {

	public final Optional<List<FPAnimation>> layers;
	public final Optional<List<FPStripe>> stripes;
	public final Optional<List<String>> filenames;

	public final OptionalInt linesPerFile;

	public FPAnimation(LuaValue lua) {
		super(lua);

		layers = FPUtils.optList(lua.get("layers"), FPAnimation::new);
		stripes = FPUtils.optList(lua.get("stripes"), FPStripe::new);
		filenames = FPUtils.optList(lua.get("filenames"), LuaValue::toString);
		linesPerFile = FPUtils.optInt(lua.get("lines_per_file"));
	}

	public void createSprites(Consumer<Sprite> consumer, int frame) {
		// TODO Auto-generated method stub

	}

	public List<Sprite> createSprites(int frame) {
		List<Sprite> ret = new ArrayList<>();
		createSprites(ret::add, frame);
		return ret;
	}
}
