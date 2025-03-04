package com.demod.fbsr.fp;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.FactorioManager;
import com.demod.fbsr.SpriteDef;
import com.google.common.collect.ImmutableList;

public class FPSpriteSheet extends FPSpriteParameters {
	public final Optional<List<FPSpriteSheet>> layers;
	public final Optional<List<String>> filenames;
	public final int variationCount;
	public final int repeatCount;
	public final int lineLength;
	public final int linesPerFile;

	public final List<SpriteDef> defs;

	public FPSpriteSheet(LuaValue lua) {
		super(lua);

		// TODO figure out how variation and repeat works

		layers = FPUtils.optList(lua.get("layers"), FPSpriteSheet::new);
		filenames = FPUtils.optList(lua.get("filenames"), LuaValue::toString);
		variationCount = lua.get("variation_count").optint(1);
		repeatCount = lua.get("repeat_count").optint(1);
		lineLength = lua.get("line_length").optint(variationCount);
		linesPerFile = lua.get("lines_per_file").optint(0);

		defs = createDefs();
	}

	private List<SpriteDef> createDefs() {
		if (layers.isPresent()) {
			return ImmutableList.of();

		} else if (filenames.isPresent()) {

			ArrayList<SpriteDef> defs = new ArrayList<SpriteDef>();
			int fileFrameCount = (linesPerFile * lineLength);
			for (int frame = 0; frame < fileFrameCount; frame++) {
				int fileFrame = frame % fileFrameCount;
				int fileIndex = frame / fileFrameCount;
				int x = this.x + width * (fileFrame % lineLength);
				int y = this.y + height * (fileFrame / lineLength);
				defs.add(SpriteDef.fromFP(filenames.get().get(fileIndex), drawAsShadow, blendMode, getEffectiveTint(),
						x, y, width, height, shift.x, shift.y, scale));
			}
			return defs;
		}

		// XXX bad hack to get image width and height
		BufferedImage image = FactorioManager.lookupModImage(filename.get());

		int frameCount = image.getHeight() / height;
		ArrayList<SpriteDef> defs = new ArrayList<SpriteDef>();
		for (int frame = 0; frame < frameCount; frame++) {
			int x = this.x + width * (frame % lineLength);
			int y = this.y + height * (frame / lineLength);

			defs.add(SpriteDef.fromFP(filename.get(), drawAsShadow, blendMode, getEffectiveTint(), x, y, width, height,
					shift.x, shift.y, scale));
		}
		return defs;
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

		consumer.accept(defs.get(frame));
	}

	public List<SpriteDef> defineSprites(int frame) {
		List<SpriteDef> ret = new ArrayList<>();
		defineSprites(ret::add, frame);
		return ret;
	}

	public int getVariationCount() {
		// So far, have seen variation/repeat counts equal across all layers
		if (layers.isPresent()) {
			// Should it be min, or max?
			return layers.get().stream().mapToInt(FPSpriteSheet::getVariationCount).min().getAsInt();
		} else {
			return Math.max(variationCount, repeatCount);
		}
	}
}
