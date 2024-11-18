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
import com.google.common.collect.ImmutableList;

public class FPRotatedAnimation extends FPAnimationParameters {

	public final Optional<List<FPRotatedAnimation>> layers;
	public final int directionCount;
	public final Optional<List<FPStripe>> stripes;
	public final Optional<List<String>> filenames;
	public final int slice;
	public final int linesPerFile;
	public final boolean applyProjection;
	public final boolean counterclockwise;

	public FPRotatedAnimation(LuaValue lua) {
		super(lua);

		layers = FPUtils.optList(lua.get("layers"), FPRotatedAnimation::new);
		directionCount = lua.get("direction_count").optint(1);
		stripes = FPUtils.optList(lua.get("stripes"), l -> new FPStripe(lua, OptionalInt.of(directionCount)));
		Optional<List<String>> filenames = FPUtils.optList(lua.get("filenames"), LuaValue::toString);
		if (!filenames.isPresent() && filename.isPresent()) {
			filenames = Optional.of(ImmutableList.of(filename.get()));
		}
		this.filenames = filenames;
		slice = lua.get("slice").optint(frameCount);
		linesPerFile = lua.get("lines_per_file").optint(0);
		applyProjection = lua.get("apply_projection").optboolean(true);
		counterclockwise = lua.get("counterclockwise").optboolean(false);
	}

	public void createSprites(Consumer<Sprite> consumer, double orientation, int frame) {
		int index = getIndex(orientation);
		createSprites(consumer, index, frame);
	}

	public void createSprites(Consumer<Sprite> consumer, int index, int frame) {
		if (layers.isPresent()) {
			for (FPRotatedAnimation animation : layers.get()) {
				animation.createSprites(consumer, index, frame);
			}
			return;
		}

		frame = index * lineLength + frame;

		if (stripes.isPresent()) {
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

	public List<Sprite> createSprites(double orientation, int frame) {
		int index = getIndex(orientation);
		return createSprites(index, frame);
	}

	public List<Sprite> createSprites(int index, int frame) {
		List<Sprite> ret = new ArrayList<>();
		createSprites(ret::add, index, frame);
		return ret;
	}

	private int getIndex(double orientation) {
		if (counterclockwise) {
			orientation = 1 - orientation;
		}
		int directionCount = this.directionCount;
		int index;
		if (applyProjection) {
			index = (int) (FPUtils.projectedOrientation(orientation) * directionCount);
		} else {
			index = (int) (orientation * directionCount);
		}
		return index;
	}

}
