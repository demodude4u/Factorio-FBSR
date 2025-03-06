package com.demod.fbsr.fp;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FBSR;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.ImageDef;
import com.demod.fbsr.SpriteDef;
import com.demod.fbsr.map.MapRect;
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

		if (stripes.isPresent()) {

			SpriteDef[] defs = new SpriteDef[directionCount * frameCount];
			int stripeRow = 0, stripeCol = 0;
			for (FPStripe stripe : stripes.get()) {

				for (int row = 0; row < stripe.heightInFrames; row++) {
					for (int col = 0; col < stripe.widthInFrames; col++) {
						int x = stripe.x + width * col;
						int y = stripe.y + height * row;

						int frame = stripeCol + col;
						int index = stripeRow + row;
						defs[index * frameCount + frame] = SpriteDef.fromFP(stripe.filename, drawAsShadow, blendMode,
								tint, applyRuntimeTint, x, y, width, height, shift.x, shift.y, scale);
					}
				}

				stripeCol += stripe.widthInFrames;
				if (stripeCol == frameCount) {
					stripeCol = 0;
					stripeRow += stripe.heightInFrames;
				}
			}

			return Arrays.asList(defs);

		} else if (filenames.isPresent()) {

			int frameCount = directionCount * lineLength;
			List<SpriteDef> defs = new ArrayList<>();
			for (int frame = 0; frame < frameCount; frame++) {

				int fileFrameCount = (linesPerFile * lineLength);
				int fileFrame = frame % fileFrameCount;
				int fileIndex = frame / fileFrameCount;
				int x = this.x + width * (fileFrame % lineLength);
				int y = this.y + height * (fileFrame / lineLength);

				defs.add(SpriteDef.fromFP(filenames.get().get(fileIndex), drawAsShadow, blendMode, tint,
						applyRuntimeTint, x, y, width, height, shift.x, shift.y, scale));

			}
			return defs;

		} else {
			int spriteCount = directionCount * frameCount;
			List<SpriteDef> defs = new ArrayList<>();
			for (int sprite = 0; sprite < spriteCount; sprite++) {
				int x = this.x + width * (sprite % lineLength);
				int y = this.y + height * (sprite / lineLength);

				defs.add(SpriteDef.fromFP(filename.get(), drawAsShadow, blendMode, tint, applyRuntimeTint, x, y, width,
						height, shift.x, shift.y, scale));
			}
			return defs;
		}
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

		if (stripes.isPresent() || filenames.isPresent()) {
			frame = index * lineLength + frame;
			consumer.accept(defs.get(frame));
			return;
		}

		frame = index * frameCount + frame;
		consumer.accept(defs.get(frame));
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

	public void getDefs(Consumer<ImageDef> consumer, int frame) {
		if (layers.isPresent()) {
			layers.get().forEach(fp -> fp.getDefs(consumer, frame));

		} else {
			for (int index = 0; index < directionCount; index++) {
				defineSprites(consumer, index, frame);
			}
		}
	}

	@Override
	public int getFrameCount() {
		if (layers.isPresent()) {
			return layers.get().stream().mapToInt(FPRotatedAnimation::getFrameCount).max().getAsInt();
		} else if (stripes.isPresent() || filenames.isPresent()) {
			return directionCount * lineLength;
		} else {
			return directionCount * frameCount;
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
