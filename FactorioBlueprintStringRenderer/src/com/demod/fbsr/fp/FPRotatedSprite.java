package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.FactorioData;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Sprite;
import com.google.common.collect.ImmutableList;

public class FPRotatedSprite extends FPSpriteParameters {

	public final Optional<List<FPRotatedSprite>> layers;
	public final int directionCount;
	public final Optional<List<String>> filenames;
	public final int linesPerFile;
	public final boolean applyProjection;
	public final boolean backEqualsFront;
	public final boolean counterclockwise;
	public final int lineLength;
	public final Optional<List<FPRotatedSpriteFrame>> frames;

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
	}

	public void createSprites(Consumer<Sprite> consumer, FactorioData data, double orientation) {
		if (layers.isPresent()) {
			for (FPRotatedSprite layer : layers.get()) {
				layer.createSprites(consumer, data, orientation);
			}
			return;
		}

		int index = getIndex(orientation);

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

		Sprite sprite = RenderUtils.createSprite(data, filename, drawAsShadow, blendMode, getEffectiveTint(), x, y,
				width, height, shiftX, shiftY, scale);
		consumer.accept(sprite);
	}

	public List<Sprite> createSprites(FactorioData data, double orientation) {
		List<Sprite> ret = new ArrayList<>();
		createSprites(ret::add, data, orientation);
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
		return index;
	}

}
