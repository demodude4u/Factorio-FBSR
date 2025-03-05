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
import com.demod.fbsr.ImageDef;
import com.demod.fbsr.SpriteDef;
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

	private final List<SpriteDef> defs;

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

		defs = createDefs();
	}

	private List<SpriteDef> createDefs() {
		if (layers.isPresent()) {
			return ImmutableList.of();
		}

		int frameCount = directionCount * lineLength;

		if (stripes.isPresent()) {

			List<SpriteDef> defs = new ArrayList<>();
			for (int frame = 0; frame < frameCount; frame++) {
				int index = frame / lineLength;

				int stripeStartIndex = 0;
				for (FPStripe stripe : stripes.get()) {

					if (stripeStartIndex + stripe.heightInFrames < index) {
						stripeStartIndex += stripe.heightInFrames;
						continue;
					}

					int stripeIndex = index - stripeStartIndex;

					// XXX bad hack to get image width and height
					BufferedImage image = FactorioManager.lookupModImage(stripe.filename);

					int width = image.getWidth() / stripe.widthInFrames;
					int height = image.getHeight() / stripe.heightInFrames;

					int x = stripe.x + width * frame;
					int y = stripe.y + height * stripeIndex;

					defs.add(SpriteDef.fromFP(stripe.filename, drawAsShadow, blendMode, tint, x, y, width, height,
							shift.x, shift.y, scale));

					break;
				}

			}

			return defs;

		} else if (filenames.isPresent()) {

			List<SpriteDef> defs = new ArrayList<>();
			for (int frame = 0; frame < frameCount; frame++) {

				int fileFrameCount = (linesPerFile * lineLength);
				int fileFrame = frame % fileFrameCount;
				int fileIndex = frame / fileFrameCount;
				int x = this.x + width * (fileFrame % lineLength);
				int y = this.y + height * (fileFrame / lineLength);

				defs.add(SpriteDef.fromFP(filenames.get().get(fileIndex), drawAsShadow, blendMode, tint, x, y, width,
						height, shift.x, shift.y, scale));

			}
			return defs;
		}

		return ImmutableList.of();
	}

	public void defineSprites(Consumer<? super SpriteDef> consumer, double orientation, int frame) {
		int index = getIndex(orientation);
		defineSprites(consumer, index, frame);
	}

	public void defineSprites(Consumer<? super SpriteDef> consumer, int index, int frame) {
		if (layers.isPresent()) {
			for (FPRotatedAnimation animation : layers.get()) {
				animation.defineSprites(consumer, index, frame);
			}
			return;
		}

		frame = index * lineLength + frame;

		if (stripes.isPresent() || filenames.isPresent()) {
			consumer.accept(defs.get(frame));
			return;
		}

		consumer.accept(defineSprite(frame));
	}

	public List<SpriteDef> defineSprites(double orientation, int frame) {
		int index = getIndex(orientation);
		return defineSprites(index, frame);
	}

	public List<SpriteDef> defineSprites(int index, int frame) {
		List<SpriteDef> ret = new ArrayList<>();
		defineSprites(ret::add, index, frame);
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

	public void getDefs(Consumer<ImageDef> consumer, int frame) {
		for (int index = 0; index < directionCount; index++) {
			defineSprites(consumer, index, frame);
		}
	}
}
