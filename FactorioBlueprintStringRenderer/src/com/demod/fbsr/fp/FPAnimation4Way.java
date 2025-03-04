package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.ImageDef;
import com.demod.fbsr.SpriteDef;
import com.google.common.collect.ImmutableList;

public class FPAnimation4Way {

	// Despite being "4Way", it actually has 8 directions
	public final Optional<FPAnimation> north;
	public final Optional<FPAnimation> northEast;
	public final Optional<FPAnimation> east;
	public final Optional<FPAnimation> southEast;
	public final Optional<FPAnimation> south;
	public final Optional<FPAnimation> southWest;
	public final Optional<FPAnimation> west;
	public final Optional<FPAnimation> northWest;

	public final Optional<FPAnimation> animation;

	public FPAnimation4Way(LuaValue lua) {
		north = FPUtils.opt(lua.get("north"), FPAnimation::new);
		northEast = FPUtils.opt(lua.get("north_east"), FPAnimation::new).or(() -> north);
		east = FPUtils.opt(lua.get("east"), FPAnimation::new).or(() -> north);
		southEast = FPUtils.opt(lua.get("south_east"), FPAnimation::new).or(() -> north);
		south = FPUtils.opt(lua.get("south"), FPAnimation::new).or(() -> north);
		southWest = FPUtils.opt(lua.get("south_west"), FPAnimation::new).or(() -> north);
		west = FPUtils.opt(lua.get("west"), FPAnimation::new).or(() -> north);
		northWest = FPUtils.opt(lua.get("north_west"), FPAnimation::new).or(() -> north);

		if (!north.isPresent()) {
			animation = FPUtils.opt(lua, FPAnimation::new);
		} else {
			animation = Optional.empty();
		}
	}

	public void defineSprites(Consumer<? super SpriteDef> consumer, Direction direction, int frame) {
		if (animation.isPresent()) {
			animation.get().defineSprites(consumer, frame);
		} else {
			List<Optional<FPAnimation>> directional = ImmutableList.of(north, northEast, east, southEast, south,
					southWest, west, northWest);
			directional.get(direction.ordinal()).get().defineSprites(consumer, frame);
		}
	}

	public List<SpriteDef> defineSprites(Direction direction, int frame) {
		List<SpriteDef> ret = new ArrayList<>();
		defineSprites(ret::add, direction, frame);
		return ret;
	}

	public void getDefs(Consumer<ImageDef> register, int frame) {
		for (Direction direction : Direction.values()) {
			defineSprites(register, direction, frame);
		}
	}
}
