package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Profile;
import com.demod.fbsr.def.SpriteDef;
import com.google.common.collect.ImmutableList;

public class FPAnimation extends FPAnimationParameters {

	public final Optional<List<FPAnimation>> layers;
	public final Optional<List<FPStripe>> stripes;
	public final Optional<List<String>> filenames;
	public final int slice;
	public final int linesPerFile;

	private final List<List<SpriteDef>> defs;

	public FPAnimation(Profile profile, LuaValue lua) {
		super(profile, lua);

		layers = FPUtils.optList(profile, lua.get("layers"), FPAnimation::new);
		stripes = FPUtils.optList(lua.get("stripes"), l -> new FPStripe(l, OptionalInt.empty()));
		filenames = FPUtils.optList(lua.get("filenames"), LuaValue::toString);
		slice = lua.get("slice").optint(frameCount);
		linesPerFile = lua.get("lines_per_file").optint(1);

		defs = createDefs(profile);
	}

	private List<List<SpriteDef>> createDefs(Profile profile) {
		if (layers.isPresent()) {
			return ImmutableList.of();

		} else if (stripes.isPresent()) {

			List<List<SpriteDef>> defs = new ArrayList<>();
			for (int frame = 0; frame < frameCount; frame++) {

				List<SpriteDef> stripeDefs = new ArrayList<>();
				for (FPStripe stripe : stripes.get()) {

					int x = stripe.x + width * (frame % stripe.widthInFrames);
					int y = stripe.y + height * (frame / stripe.widthInFrames);

					stripeDefs.add(SpriteDef.fromFP(profile, stripe.filename, drawAsShadow, blendMode, tint, tintAsOverlay,
							applyRuntimeTint, x, y, width, height, shift.x, shift.y, scale));
				}
				defs.add(stripeDefs);
			}

			return defs;

		} else if (filenames.isPresent()) {

			List<List<SpriteDef>> defs = new ArrayList<>();
			for (int frame = 0; frame < frameCount; frame++) {
				int fileFrameCount = (linesPerFile * lineLength);
				int fileFrame = frame % fileFrameCount;
				int fileIndex = frame / fileFrameCount;
				int x = this.x + width * (fileFrame % lineLength);
				int y = this.y + height * (fileFrame / lineLength);

				defs.add(ImmutableList.of(SpriteDef.fromFP(profile, filenames.get().get(fileIndex), drawAsShadow, blendMode,
						tint, tintAsOverlay, applyRuntimeTint, x, y, width, height, shift.x, shift.y, scale)));
			}
			return defs;
		}

		return ImmutableList.of();
	}

	public void defineSprites(Consumer<? super SpriteDef> consumer, int frame) {
		if (layers.isPresent()) {
			for (FPAnimation animation : layers.get()) {
				animation.defineSprites(consumer, frame);
			}
			return;

		} else if (stripes.isPresent() || filenames.isPresent()) {
			defs.get(frame).forEach(consumer);
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
