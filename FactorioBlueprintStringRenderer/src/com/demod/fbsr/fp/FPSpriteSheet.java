package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Profile;
import com.demod.fbsr.def.SpriteDef;
import com.google.common.collect.ImmutableList;

public class FPSpriteSheet extends FPSpriteParameters {
	public final Optional<List<FPSpriteSheet>> layers;
	public final Optional<List<String>> filenames;
	public final int variationCount;
	public final int repeatCount;
	public final int lineLength;
	public final int linesPerFile;

	public final List<SpriteDef> defs;

	public FPSpriteSheet(Profile profile, LuaValue lua) {
		super(profile, lua);

		layers = FPUtils.optList(profile, lua.get("layers"), FPSpriteSheet::new);
		filenames = FPUtils.optList(lua.get("filenames"), LuaValue::toString);
		variationCount = lua.get("variation_count").optint(1);
		repeatCount = lua.get("repeat_count").optint(1);
		lineLength = lua.get("line_length").optint(variationCount);
		linesPerFile = lua.get("lines_per_file").optint(0);

		// TODO figure out how variation and repeat works
		if (variationCount > 1 && repeatCount > 1) {
			throw new RuntimeException("Look into how variation count and repeat count works!");
		}

		defs = createDefs(profile);
		FPUtils.verifyNotNull(lua.getDebugPath() + " defs", defs);
	}

	private List<SpriteDef> createDefs(Profile profile) {
		if (layers.isPresent()) {
			return ImmutableList.of();

		} else if (filenames.isPresent()) {
			ArrayList<SpriteDef> defs = new ArrayList<SpriteDef>();
			int frameCount = variationCount;
			int fileFrameCount = (linesPerFile * lineLength);
			for (int frame = 0; frame < frameCount; frame++) {
				int fileFrame = frame % fileFrameCount;
				int fileIndex = frame / fileFrameCount;
				int x = this.x + width * (fileFrame % lineLength);
				int y = this.y + height * (fileFrame / lineLength);
				defs.add(SpriteDef.fromFP(profile, filenames.get().get(fileIndex), drawAsShadow, blendMode, tint, tintAsOverlay,
						applyRuntimeTint, x, y, width, height, shift.x, shift.y, scale));
			}
			return defs;

		} else {
			int frameCount = variationCount;
			ArrayList<SpriteDef> defs = new ArrayList<SpriteDef>();
			for (int frame = 0; frame < frameCount; frame++) {
				int x = this.x + width * (frame % lineLength);
				int y = this.y + height * (frame / lineLength);

				defs.add(SpriteDef.fromFP(profile, filename.get(), drawAsShadow, blendMode, tint, tintAsOverlay,
						applyRuntimeTint, x, y, width, height, shift.x, shift.y, scale));
			}
			return defs;
		}
	}

	public void defineSprites(Consumer<? super SpriteDef> consumer, int frame) {
		if (layers.isPresent()) {
			for (FPSpriteSheet animation : layers.get()) {
				animation.defineSprites(consumer, frame);
			}
			return;

		} else if (filenames.isPresent()) {

			consumer.accept(defs.get(frame));
			return;
		}

		// TODO figure out the exact dynamic of variation_count and repeat_count
		consumer.accept(defs.get(frame % variationCount));
	}

	public List<SpriteDef> defineSprites(int frame) {
		List<SpriteDef> ret = new ArrayList<>();
		defineSprites(ret::add, frame);
		return ret;
	}

	public int getFrameCount() {
		// So far, have seen variation/repeat counts equal across all layers
		if (layers.isPresent()) {
			// Should it be min, or max?
			return layers.get().stream().mapToInt(FPSpriteSheet::getFrameCount).min().getAsInt();
		} else {
			return Math.max(variationCount, repeatCount);
		}
	}
}
