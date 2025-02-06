package com.demod.fbsr.fp;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;

import com.demod.factorio.FactorioData;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Sprite;

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
		stripes = FPUtils.optList(lua.get("stripes"), l -> new FPStripe(l, OptionalInt.of(directionCount)));
		filenames = FPUtils.optList(lua.get("filenames"), LuaValue::toString);
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

			int stripeStartIndex = 0;
			for (FPStripe stripe : stripes.get()) {

				if (stripeStartIndex + stripe.heightInFrames < index) {
					stripeStartIndex += stripe.heightInFrames;
					continue;
				}

				int stripeIndex = index - stripeStartIndex;

				// XXX at least it is cached
				BufferedImage image = FactorioData.getModImage(stripe.filename);

				int width = image.getWidth() / stripe.widthInFrames;
				int height = image.getHeight() / stripe.heightInFrames;

				int x = stripe.x + width * frame;
				int y = stripe.y + height * stripeIndex;

				consumer.accept(RenderUtils.createSprite(stripe.filename, drawAsShadow, blendMode, getEffectiveTint(),
						x, y, width, height, shift.x, shift.y, scale));

				break;
			}

			return;

		} else if (filenames.isPresent()) {

			int fileFrameCount = (linesPerFile * lineLength);
			int fileFrame = frame % fileFrameCount;
			int fileIndex = frame / fileFrameCount;
			int x = this.x + width * (fileFrame % lineLength);
			int y = this.y + height * (fileFrame / lineLength);

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

	@Override
	public int getFrameCount() {
		if (layers.isPresent()) {
			return layers.get().stream().mapToInt(FPRotatedAnimation::getFrameCount).max().getAsInt();
		} else {
			// Do I need something different for stripes or filenames?
			return super.getFrameCount();
		}
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
