package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.FactorioData;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Sprite;

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

	public void createSprites(Consumer<Sprite> consumer, FactorioData data, int variation, int frame) {

		// TODO assumptions made with variation, needs testing
		frame = variation * lineLength + frame;

		if (filenames.isPresent()) {

			// TODO how do I use slice?

			int fileFrameCount = (linesPerFile * lineLength);
			int fileFrame = frame % fileFrameCount;
			int fileIndex = frame / fileFrameCount;
			int x = width * (fileFrame % lineLength);
			int y = height * (fileFrame / lineLength);

			consumer.accept(RenderUtils.createSprite(data, filenames.get().get(fileIndex), drawAsShadow, blendMode,
					getEffectiveTint(), x, y, width, height, shift.x, shift.y, scale));
			return;
		}

		int x = width * (frame % lineLength);
		int y = height * (frame / lineLength);

		consumer.accept(RenderUtils.createSprite(data, filename.get(), drawAsShadow, blendMode, getEffectiveTint(), x,
				y, width, height, shift.x, shift.y, scale));
	}

	public List<Sprite> createSprites(FactorioData data, int variation, int frame) {
		List<Sprite> ret = new ArrayList<>();
		createSprites(ret::add, data, variation, frame);
		return ret;
	}

}
