package com.demod.fbsr.fp;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.FactorioManager;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.SpriteDef;

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

	// TODO stripes are a troublesome structure, we need a better way
	public void defineSprites(Consumer<SpriteDef> consumer, int frame) {
		if (layers.isPresent()) {
			for (FPAnimation animation : layers.get()) {
				animation.defineSprites(consumer, frame);
			}
			return;

		} else if (stripes.isPresent()) {
			for (FPStripe stripe : stripes.get()) {

				// XXX at least it is cached
				String firstSegment = stripe.filename.split("\\/")[0];
				String modName = firstSegment.substring(2, firstSegment.length() - 2);
				BufferedImage image = FactorioManager.lookupDataByModName(modName).get(0).getModImage(stripe.filename);

				// TODO do I ignore width/height in Animation proto?
				int width = image.getWidth() / stripe.widthInFrames;
				int height = image.getHeight() / stripe.heightInFrames;

				int x = stripe.x + width * (frame % stripe.widthInFrames);
				int y = stripe.y + height * (frame / stripe.heightInFrames);

				consumer.accept(RenderUtils.defineSprite(stripe.filename, drawAsShadow, blendMode, getEffectiveTint(),
						x, y, width, height, shift.x, shift.y, scale));
			}

			return;

		} else if (filenames.isPresent()) {

			// TODO how do I use slice?

			int fileFrameCount = (linesPerFile * lineLength);
			int fileFrame = frame % fileFrameCount;
			int fileIndex = frame / fileFrameCount;
			int x = this.x + width * (fileFrame % lineLength);
			int y = this.y + height * (fileFrame / lineLength);

			consumer.accept(RenderUtils.defineSprite(filenames.get().get(fileIndex), drawAsShadow, blendMode,
					getEffectiveTint(), x, y, width, height, shift.x, shift.y, scale));
			return;
		}

		consumer.accept(defineSprite(frame));
	}

	public List<SpriteDef> defineSprites(int frame) {
		List<SpriteDef> ret = new ArrayList<>();
		defineSprites(ret::add, frame);
		return ret;
	}
}
