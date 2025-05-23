package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.ModsProfile;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.SpriteDef;

public class FPAnimationSheet extends FPAnimationParameters {

	public final int variationCount;
	public final Optional<List<String>> filenames;
	public final int linesPerFile;

	private final List<SpriteDef> defs;

	public FPAnimationSheet(ModsProfile profile, LuaValue lua) {
		super(profile, lua);

		variationCount = lua.get("variation_count").checkint();
		filenames = FPUtils.optList(lua.get("filenames"), LuaValue::toString);
		linesPerFile = lua.get("lines_per_file").optint(0);

		defs = createDefs(profile);
	}

	private List<SpriteDef> createDefs(ModsProfile profile) {

		List<SpriteDef> defs = new ArrayList<>();
		int frameCount = variationCount * lineLength;

		if (filenames.isPresent()) {

			for (int frame = 0; frame < frameCount; frame++) {

				int fileFrameCount = (linesPerFile * lineLength);
				int fileFrame = frame % fileFrameCount;
				int fileIndex = frame / fileFrameCount;
				int x = width * (fileFrame % lineLength);
				int y = height * (fileFrame / lineLength);

				defs.add(SpriteDef.fromFP(profile, filenames.get().get(fileIndex), drawAsShadow, blendMode, tint, tintAsOverlay,
						applyRuntimeTint, x, y, width, height, shift.x, shift.y, scale));
			}
			return defs;
		}

		for (int frame = 0; frame < frameCount; frame++) {
			int x = width * (frame % lineLength);
			int y = height * (frame / lineLength);

			defs.add(SpriteDef.fromFP(profile, filename.get(), drawAsShadow, blendMode, tint, tintAsOverlay, applyRuntimeTint, x,
					y, width, height, shift.x, shift.y, scale));
		}
		return defs;
	}

	public void defineSprites(Consumer<? super SpriteDef> consumer, int variation, int frame) {
		frame = variation * lineLength + frame;
		consumer.accept(defs.get(frame));
	}

	public List<SpriteDef> defineSprites(int variation, int frame) {
		List<SpriteDef> ret = new ArrayList<>();
		defineSprites(ret::add, variation, frame);
		return ret;
	}

	public void getDefs(Consumer<ImageDef> register, int frame) {
		for (int variation = 0; variation < variationCount; variation++) {
			defineSprites(register, variation, frame);
		}
	}

}
