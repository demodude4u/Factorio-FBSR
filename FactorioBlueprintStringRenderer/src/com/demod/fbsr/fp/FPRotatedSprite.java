package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Profile;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.SpriteDef;
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

	protected final List<SpriteDef> defs;

	private final int limitedDirectionCount;
	
	public FPRotatedSprite(Profile profile, LuaValue lua) {
		this(profile, lua, Optional.empty(), Integer.MAX_VALUE);
	}

	public FPRotatedSprite(Profile profile, LuaValue lua, int limitDirectionCount) {
		this(profile, lua, Optional.empty(), limitDirectionCount);
	}

	public FPRotatedSprite(Profile profile, LuaValue lua, Optional<Boolean> overrideBackEqualsFront) {
		this(profile, lua, overrideBackEqualsFront, Integer.MAX_VALUE);
	}

	public FPRotatedSprite(Profile profile, LuaValue lua, Optional<Boolean> overrideBackEqualsFront, int limitDirectionCount) {
		this(profile, lua, overrideBackEqualsFront, limitDirectionCount, (p, l) -> new FPRotatedSprite(p, l, overrideBackEqualsFront, limitDirectionCount));
	}

	//For Sloped Sprites
	protected FPRotatedSprite(Profile profile, LuaValue lua, BiFunction<Profile, LuaValue, FPRotatedSprite> layerFactory) {
		this(profile, lua, Optional.empty(), Integer.MAX_VALUE, layerFactory);
	}

	private FPRotatedSprite(Profile profile, LuaValue lua, Optional<Boolean> overrideBackEqualsFront, int limitDirectionCount, BiFunction<Profile, LuaValue, FPRotatedSprite> layerFactory) {
		super(profile, lua);

		layers = FPUtils.optList(profile, lua.get("layers"),layerFactory);
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

		this.limitedDirectionCount = Math.min(limitDirectionCount, directionCount);
		List<SpriteDef> allDefs = createDefs(profile);
		defs = limitedDirectionDefs(allDefs);
	}

	private List<SpriteDef> createDefs(Profile profile) {
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

			defs.add(SpriteDef.fromFP(profile, filename, drawAsShadow, blendMode, tint, tintAsOverlay, applyRuntimeTint, x, y,
					width, height, shiftX, shiftY, scale));
		}

		return defs;
	}

	private List<SpriteDef> limitedDirectionDefs(List<SpriteDef> allDefs) {
		if (limitedDirectionCount == directionCount || allDefs.isEmpty()) {
			return allDefs;
		}

		return IntStream.range(0, limitedDirectionCount).map(i -> (i * directionCount) / limitedDirectionCount)
				.mapToObj(allDefs::get).collect(Collectors.toList());
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

	public void getDefs(Consumer<ImageDef> register) {
		if (layers.isPresent()) {
			layers.get().forEach(fp -> fp.getDefs(register));
		}

		defs.forEach(register);
	}

	private int getIndex(double orientation) {
		if (counterclockwise) {
			orientation = 1 - orientation;
		}
		int directionCount = this.limitedDirectionCount;
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

	public int getLimitedDirectionCount() {
		return limitedDirectionCount;
	}

	public double getAlignedOrientation(double orientation) {
		if (layers.isPresent()) {
			return layers.get().get(0).getAlignedOrientation(orientation);
		}
		return getIndex(orientation) / (double) limitedDirectionCount;
	}
}
