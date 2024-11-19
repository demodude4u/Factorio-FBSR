package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.fbsr.FPUtils;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Sprite;

public class FPSpriteSheet extends FPSpriteParameters {
	public final Optional<List<FPSpriteSheet>> layers;
	public final Optional<List<String>> filenames;
	public final int variationCount;
	public final int repeatCount;
	public final int lineLength;
	public final int linesPerFile;

	public FPSpriteSheet(LuaValue lua) {
		super(lua);

		// TODO figure out how variation and repeat works

		layers = FPUtils.optList(lua.get("layers"), FPSpriteSheet::new);
		filenames = FPUtils.optList(lua.get("filenames"), LuaValue::toString);
		variationCount = lua.get("variation_count").optint(1);
		repeatCount = lua.get("repeat_count").optint(1);
		lineLength = lua.get("line_length").optint(variationCount);
		linesPerFile = lua.get("lines_per_file").optint(0);
	}

	public void createSprites(Consumer<Sprite> consumer, int frame) {
		if (layers.isPresent()) {
			for (FPSpriteSheet animation : layers.get()) {
				animation.createSprites(consumer, frame);
			}
			return;

		} else if (filenames.isPresent()) {

			// TODO how do I use slice?

			int fileFrameCount = (linesPerFile * lineLength);
			int fileFrame = frame % fileFrameCount;
			int fileIndex = frame / fileFrameCount;
			int x = this.x + width * (fileFrame % lineLength);
			int y = this.y + height * (fileFrame / lineLength);

			consumer.accept(RenderUtils.createSprite(filenames.get().get(fileIndex), drawAsShadow, blendMode,
					getEffectiveTint(), x, y, width, height, shift.x, shift.y, scale));
			return;
		}

		int x = this.x + width * (frame % lineLength);
		int y = this.y + height * (frame / lineLength);

		consumer.accept(RenderUtils.createSprite(filename.get(), drawAsShadow, blendMode, getEffectiveTint(), x, y,
				width, height, shift.x, shift.y, scale));
	}

	public List<Sprite> createSprites(int frame) {
		List<Sprite> ret = new ArrayList<>();
		createSprites(ret::add, frame);
		return ret;
	}
}
