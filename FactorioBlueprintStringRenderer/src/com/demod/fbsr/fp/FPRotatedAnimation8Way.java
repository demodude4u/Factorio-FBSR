package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.fbsr.Direction;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Sprite;
import com.google.common.collect.ImmutableList;

// Most cursed
public class FPRotatedAnimation8Way {

	public final Optional<FPRotatedAnimation> north;
	public final Optional<FPRotatedAnimation> northEast;
	public final Optional<FPRotatedAnimation> east;
	public final Optional<FPRotatedAnimation> southEast;
	public final Optional<FPRotatedAnimation> south;
	public final Optional<FPRotatedAnimation> southWest;
	public final Optional<FPRotatedAnimation> west;
	public final Optional<FPRotatedAnimation> northWest;

	public final Optional<FPRotatedAnimation> animation;

	public FPRotatedAnimation8Way(LuaValue lua) {
		north = FPUtils.opt(lua.get("north"), FPRotatedAnimation::new);
		Optional<FPRotatedAnimation> northEast = FPUtils.opt(lua.get("north_east"), FPRotatedAnimation::new);
		Optional<FPRotatedAnimation> east = FPUtils.opt(lua.get("east"), FPRotatedAnimation::new);
		Optional<FPRotatedAnimation> southEast = FPUtils.opt(lua.get("south_east"), FPRotatedAnimation::new);
		Optional<FPRotatedAnimation> south = FPUtils.opt(lua.get("south"), FPRotatedAnimation::new);
		Optional<FPRotatedAnimation> southWest = FPUtils.opt(lua.get("south_west"), FPRotatedAnimation::new);
		Optional<FPRotatedAnimation> west = FPUtils.opt(lua.get("west"), FPRotatedAnimation::new);
		Optional<FPRotatedAnimation> northWest = FPUtils.opt(lua.get("north_west"), FPRotatedAnimation::new);

		// If not defined, try the opposite direction or north
		this.northEast = northEast.or(() -> southWest).or(() -> north);
		this.east = east.or(() -> west).or(() -> north);
		this.southEast = southWest.or(() -> northWest).or(() -> north);
		this.south = south.or(() -> north);
		this.southWest = southWest.or(() -> northEast).or(() -> north);
		this.west = west.or(() -> east).or(() -> north);
		this.northWest = northWest.or(() -> southEast).or(() -> north);

		if (!north.isPresent()) {
			animation = FPUtils.opt(lua, FPRotatedAnimation::new);
		} else {
			animation = Optional.empty();
		}
	}

	public void createSprites(Consumer<Sprite> consumer, Direction direction, double orientation, int frame) {
		if (animation.isPresent()) {
			animation.get().createSprites(consumer, orientation, frame);
		} else {
			List<Optional<FPRotatedAnimation>> directional = ImmutableList.of(north, northEast, east, southEast, south,
					southWest, west, northWest);
			directional.get(direction.ordinal()).get().createSprites(consumer, orientation, frame);
		}
	}

	public List<Sprite> createSprites(Direction direction, double orientation, int frame) {
		List<Sprite> ret = new ArrayList<>();
		createSprites(ret::add, direction, orientation, frame);
		return ret;
	}

}
