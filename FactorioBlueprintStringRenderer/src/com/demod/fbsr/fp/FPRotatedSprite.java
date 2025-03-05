package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.ImageDef;
import com.demod.fbsr.SpriteDef;
import com.google.common.collect.ImmutableList;

public class FPRotatedSprite extends FPSpriteParameters {
	private static final Logger LOGGER = LoggerFactory.getLogger(FPRotatedSprite.class);

	public final Optional<List<FPRotatedSprite>> layers;
	public final int directionCount;
	public final Optional<List<String>> filenames;
	public final int linesPerFile;
	public final boolean applyProjection;
	public final boolean backEqualsFront;
	public final boolean counterclockwise;
	public final int lineLength;
	public final Optional<List<FPRotatedSpriteFrame>> frames;

	private final List<SpriteDef> defs;

	public FPRotatedSprite(LuaValue lua) {
		this(lua, Optional.empty());
	}

	public FPRotatedSprite(LuaValue lua, Optional<Boolean> overrideBackEqualsFront) {
		super(lua);

		layers = FPUtils.optList(lua.get("layers"), l -> new FPRotatedSprite(l, overrideBackEqualsFront));
		directionCount = lua.get("direction_count").optint(1);
		Optional<List<String>> filenames = FPUtils.optList(lua.get("filenames"), LuaValue::tojstring);
		if (!filenames.isPresent() && filename.isPresent()) {
			filenames = Optional.of(ImmutableList.of(filename.get()));
		}
		this.filenames = filenames;
		linesPerFile = lua.get("lines_per_file").optint(0);
		applyProjection = lua.get("apply_projection").optboolean(true);
		// XXX electric poles should have back_equals_front, but they do not?
		if (overrideBackEqualsFront.isPresent()) {
			this.backEqualsFront = overrideBackEqualsFront.get();
		} else {
			this.backEqualsFront = lua.get("back_equals_front").optboolean(false);
		}
		counterclockwise = lua.get("counterclockwise").optboolean(false);
		lineLength = lua.get("line_length").optint(0);
		frames = FPUtils.optList(lua.get("frames"), l -> new FPRotatedSpriteFrame(lua, width, height));

		defs = createDefs();
	}

	private List<SpriteDef> createDefs() {
		if (layers.isPresent()) {
			return ImmutableList.of();
		}

		List<SpriteDef> defs = new ArrayList<>();

		for (int index = 0; index < directionCount; index++) {

			int x = this.x;
			int y = this.y;
			int fileIndex;
			int tileIndex;
			if (lineLength == 0 || linesPerFile == 0) {
				fileIndex = 0;
				tileIndex = index;
			} else {
				int fileLength = lineLength * linesPerFile;
				fileIndex = index / fileLength;
				tileIndex = index % fileLength;
			}
			if (fileIndex >= filenames.get().size()) {
				LOGGER.warn("Warning: Trying to access sprite {} in {} files", index, filenames.get().size());
			}
			String filename = filenames.get().get(fileIndex);
			if (lineLength > 0) {
				x += (tileIndex % lineLength) * width;
				y += (tileIndex / lineLength) * height;
			} else {
				x += tileIndex * width;
			}

			int width = this.width;
			int height = this.height;
			double shiftX = shift.x;
			double shiftY = shift.y;
			if (frames.isPresent()) {
				FPRotatedSpriteFrame frame = frames.get().get(index);
				x += frame.x;
				y += frame.y;
				width = frame.width;
				height = frame.height;
				shiftX += frame.shift.x;
				shiftY += frame.shift.y;
			}

			defs.add(SpriteDef.fromFP(filename, drawAsShadow, blendMode, tint, x, y, width, height, shiftX, shiftY,
					scale));
		}

		return defs;
	}

	public void defineSprites(Consumer<? super SpriteDef> consumer, double orientation) {
		if (layers.isPresent()) {
			for (FPRotatedSprite layer : layers.get()) {
				layer.defineSprites(consumer, orientation);
			}
			return;
		}

		int index = getIndex(orientation);
		consumer.accept(defs.get(index));
	}

	public List<SpriteDef> defineSprites(double orientation) {
		List<SpriteDef> ret = new ArrayList<>();
		defineSprites(ret::add, orientation);
		return ret;
	}

	private int getIndex(double orientation) {
		if (counterclockwise) {
			orientation = 1 - orientation;
		}
		int directionCount = this.directionCount;
		if (backEqualsFront) {
			directionCount *= 2;
		}
		int index;
		if (applyProjection) {
			index = (int) Math.round(FPUtils.projectedOrientation(orientation) * directionCount);
		} else {
			index = (int) Math.round(orientation * directionCount);
		}
		if (backEqualsFront) {
			index = index % (directionCount / 2);
		}
		if (index == directionCount) {
			index = 0;
		}
		return index;
	}

	public void getDefs(Consumer<ImageDef> register) {
		defs.forEach(register);
	}

}
