package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.ModsProfile;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.SpriteDef;
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

	private List<Optional<FPRotatedAnimation>> directional;

	public FPRotatedAnimation8Way(ModsProfile profile, LuaValue lua) {
		north = FPUtils.opt(profile, lua.get("north"), FPRotatedAnimation::new);
		Optional<FPRotatedAnimation> northEast = FPUtils.opt(profile, lua.get("north_east"), FPRotatedAnimation::new);
		Optional<FPRotatedAnimation> east = FPUtils.opt(profile, lua.get("east"), FPRotatedAnimation::new);
		Optional<FPRotatedAnimation> southEast = FPUtils.opt(profile, lua.get("south_east"), FPRotatedAnimation::new);
		Optional<FPRotatedAnimation> south = FPUtils.opt(profile, lua.get("south"), FPRotatedAnimation::new);
		Optional<FPRotatedAnimation> southWest = FPUtils.opt(profile, lua.get("south_west"), FPRotatedAnimation::new);
		Optional<FPRotatedAnimation> west = FPUtils.opt(profile, lua.get("west"), FPRotatedAnimation::new);
		Optional<FPRotatedAnimation> northWest = FPUtils.opt(profile, lua.get("north_west"), FPRotatedAnimation::new);

		// If not defined, try the opposite direction or north
		this.northEast = northEast.or(() -> southWest).or(() -> north);
		this.east = east.or(() -> west).or(() -> north);
		this.southEast = southWest.or(() -> northWest).or(() -> north);
		this.south = south.or(() -> north);
		this.southWest = southWest.or(() -> northEast).or(() -> north);
		this.west = west.or(() -> east).or(() -> north);
		this.northWest = northWest.or(() -> southEast).or(() -> north);

		if (!north.isPresent()) {
			animation = FPUtils.opt(profile, lua, FPRotatedAnimation::new);
		} else {
			animation = Optional.empty();
			directional = ImmutableList.of(this.north, this.northEast, this.east, this.southEast, this.south,
					this.southWest, this.west, this.northWest);
		}
	}

	public void defineSprites(Consumer<? super SpriteDef> consumer, Direction direction, double orientation,
			int frame) {
		if (animation.isPresent()) {
			animation.get().defineSprites(consumer, orientation, frame);
		} else {
			directional.get(direction.ordinal()).get().defineSprites(consumer, orientation, frame);
		}
	}

	public List<SpriteDef> defineSprites(Direction direction, double orientation, int frame) {
		List<SpriteDef> ret = new ArrayList<>();
		defineSprites(ret::add, direction, orientation, frame);
		return ret;
	}

	public void getDefs(Consumer<ImageDef> register, Direction direction, int frame) {
		if (animation.isPresent()) {
			animation.get().getDefs(register, frame);
		} else {
			directional.get(direction.ordinal()).get().getDefs(register, frame);
		}
	}

	public void getDefs(Consumer<ImageDef> register, double orientation, int frame) {
		if (animation.isPresent()) {
			animation.get().defineSprites(register, orientation, frame);
		} else {
			directional.forEach(fp -> fp.get().defineSprites(register, orientation, frame));
		}
	}

	public void getDefs(Consumer<ImageDef> register, int frame) {
		if (animation.isPresent()) {
			animation.get().getDefs(register, frame);
		} else {
			directional.forEach(fp -> fp.get().getDefs(register, frame));
		}
	}

}
