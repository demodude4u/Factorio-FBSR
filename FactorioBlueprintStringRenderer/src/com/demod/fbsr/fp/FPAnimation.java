package com.demod.fbsr.fp;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.FactorioData;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Sprite;

public class FPAnimation extends FPAnimationParameters {

	public final Optional<List<FPAnimation>> layers;
	public final Optional<List<FPStripe>> stripes;
	public final Optional<List<String>> filenames;
	public final int slice;
	public final int linesPerFile;

	public FPAnimation(LuaValue lua) {
		super(lua);

		layers = FPUtils.optList(lua.get("layers"), FPAnimation::new);
		stripes = FPUtils.optList(lua.get("stripes"), l -> new FPStripe(lua, OptionalInt.empty()));
		filenames = FPUtils.optList(lua.get("filenames"), LuaValue::toString);
		slice = lua.get("slice").optint(frameCount);
		linesPerFile = lua.get("lines_per_file").optint(1);
	}

	public void createSprites(Consumer<Sprite> consumer, int frame) {
		if (layers.isPresent()) {
			for (FPAnimation animation : layers.get()) {
				animation.createSprites(consumer, frame);
			}
			return;

		} else if (stripes.isPresent()) {
			for (FPStripe stripe : stripes.get()) {

				// XXX at least it is cached
				BufferedImage image = FactorioData.getModImage(stripe.filename);

				// TODO do I ignore width/height in Animation proto?
				int width = image.getWidth() / stripe.widthInFrames;
				int height = image.getHeight() / stripe.heightInFrames;

				// TODO how does X and Y work in Stripe?
				int x = width * (frame % stripe.widthInFrames);
				int y = height * (frame / stripe.heightInFrames);

				consumer.accept(RenderUtils.createSprite(stripe.filename, drawAsShadow, blendMode, getEffectiveTint(),
						x, y, width, height, shift.x, shift.y, scale));
			}

			return;

		} else if (filenames.isPresent()) {

			// TODO how do I use slice?

			int fileFrameCount = (linesPerFile * lineLength);
			int fileFrame = frame % fileFrameCount;
			int fileIndex = frame / fileFrameCount;
			int x = width * (fileFrame % lineLength);
			int y = height * (fileFrame / lineLength);

			consumer.accept(RenderUtils.createSprite(filenames.get().get(fileIndex), drawAsShadow, blendMode,
					getEffectiveTint(), x, y, width, height, shift.x, shift.y, scale));
			return;
		}

		consumer.accept(createSprite(frame));
	}

	public List<Sprite> createSprites(int frame) {
		List<Sprite> ret = new ArrayList<>();
		createSprites(ret::add, frame);
		return ret;
	}
}
