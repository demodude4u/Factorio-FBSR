package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.SpriteDef;

public class FPAnimationSheet extends FPAnimationParameters {

	public final int variationCount;
	public final Optional<List<String>> filenames;
	public final int linesPerFile;

	public FPAnimationSheet(LuaValue lua) {
		super(lua);

		variationCount = lua.get("variation_count").checkint();
		filenames = FPUtils.optList(lua.get("filenames"), LuaValue::toString);
		linesPerFile = lua.get("lines_per_file").optint(0);
	}

	public void defineSprites(Consumer<SpriteDef> consumer, int variation, int frame) {

		// TODO assumptions made with variation, needs testing
		frame = variation * lineLength + frame;

		if (filenames.isPresent()) {

			// TODO how do I use slice?

			int fileFrameCount = (linesPerFile * lineLength);
			int fileFrame = frame % fileFrameCount;
			int fileIndex = frame / fileFrameCount;
			int x = width * (fileFrame % lineLength);
			int y = height * (fileFrame / lineLength);

			consumer.accept(RenderUtils.defineSprite(filenames.get().get(fileIndex), drawAsShadow, blendMode,
					getEffectiveTint(), x, y, width, height, shift.x, shift.y, scale));
			return;
		}

		int x = width * (frame % lineLength);
		int y = height * (frame / lineLength);

		consumer.accept(RenderUtils.defineSprite(filename.get(), drawAsShadow, blendMode, getEffectiveTint(), x,
				y, width, height, shift.x, shift.y, scale));
	}

	public List<SpriteDef> defineSprites(int variation, int frame) {
		List<SpriteDef> ret = new ArrayList<>();
		defineSprites(ret::add, variation, frame);
		return ret;
	}

}
