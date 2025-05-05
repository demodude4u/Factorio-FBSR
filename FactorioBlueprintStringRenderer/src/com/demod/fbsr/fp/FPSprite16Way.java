package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Dir16;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.SpriteDef;

public class FPSprite16Way {

	public final Optional<FPSprite> north;
	public final Optional<FPSprite> north_north_east;
	public final Optional<FPSprite> north_east;
	public final Optional<FPSprite> east_north_east;
	public final Optional<FPSprite> east;
	public final Optional<FPSprite> east_south_east;
	public final Optional<FPSprite> south_east;
	public final Optional<FPSprite> south_south_east;
	public final Optional<FPSprite> south;
	public final Optional<FPSprite> south_south_west;
	public final Optional<FPSprite> south_west;
	public final Optional<FPSprite> west_south_west;
	public final Optional<FPSprite> west;
	public final Optional<FPSprite> west_north_west;
	public final Optional<FPSprite> north_west;
	public final Optional<FPSprite> north_north_west;

	public final Optional<FPSpriteNWaySheet> sheet;
	public final Optional<List<FPSpriteNWaySheet>> sheets;
	public final Optional<FPSprite> sprite;
	private final List<List<SpriteDef>> defs;

	public FPSprite16Way(LuaValue lua) {
		north = FPUtils.opt(lua.get("north"), FPSprite::new);
		north_north_east = FPUtils.opt(lua.get("north_north_east"), FPSprite::new);
		north_east = FPUtils.opt(lua.get("north_east"), FPSprite::new);
		east_north_east = FPUtils.opt(lua.get("east_north_east"), FPSprite::new);
		east = FPUtils.opt(lua.get("east"), FPSprite::new);
		east_south_east = FPUtils.opt(lua.get("east_south_east"), FPSprite::new);
		south_east = FPUtils.opt(lua.get("south_east"), FPSprite::new);
		south_south_east = FPUtils.opt(lua.get("south_south_east"), FPSprite::new);
		south = FPUtils.opt(lua.get("south"), FPSprite::new);
		south_south_west = FPUtils.opt(lua.get("south_south_west"), FPSprite::new);
		south_west = FPUtils.opt(lua.get("south_west"), FPSprite::new);
		west_south_west = FPUtils.opt(lua.get("west_south_west"), FPSprite::new);
		west = FPUtils.opt(lua.get("west"), FPSprite::new);
		west_north_west = FPUtils.opt(lua.get("west_north_west"), FPSprite::new);
		north_west = FPUtils.opt(lua.get("north_west"), FPSprite::new);
		north_north_west = FPUtils.opt(lua.get("north_north_west"), FPSprite::new);

		sheet = FPSpriteNWaySheet.opt(lua.get("sheet"), 16);
		sheets = FPUtils.optList(lua.get("sheets"), l -> new FPSpriteNWaySheet(l, 16));

		if (!sheet.isPresent() && !sheets.isPresent() && !north.isPresent()) {
			sprite = FPUtils.opt(lua, FPSprite::new);
		} else {
			sprite = Optional.empty();
		}

		defs = createDefs();
	}

	private List<List<SpriteDef>> createDefs() {
		List<List<SpriteDef>> defs = new ArrayList<>();
		if (sprite.isPresent()) {
			defs.add(new ArrayList<>());
			sprite.get().defineSprites(defs.get(0)::add);
		} else {
			for (Dir16 direction : Dir16.values()) {
				defs.add(new ArrayList<>());
				if (sheets.isPresent()) {
					sheets.get().stream().forEach(s -> defs.get(defs.size() - 1).add(s.defineSprite(direction)));
				} else if (sheet.isPresent()) {
					defs.get(defs.size() - 1).add(sheet.get().defineSprite(direction));
				} else {
					FPSprite dirSprite = null;
					if (direction == Dir16.N) {
						dirSprite = north.get();
					} else if (direction == Dir16.NNE) {
						dirSprite = north_north_east.get();
					} else if (direction == Dir16.NE) {
						dirSprite = north_east.get();
					} else if (direction == Dir16.ENE) {
						dirSprite = east_north_east.get();
					} else if (direction == Dir16.E) {
						dirSprite = east.get();
					} else if (direction == Dir16.ESE) {
						dirSprite = east_south_east.get();
					} else if (direction == Dir16.SE) {
						dirSprite = south_east.get();
					} else if (direction == Dir16.SSE) {
						dirSprite = south_south_east.get();
					} else if (direction == Dir16.S) {
						dirSprite = south.get();
					} else if (direction == Dir16.SSW) {
						dirSprite = south_south_west.get();
					} else if (direction == Dir16.SW) {
						dirSprite = south_west.get();
					} else if (direction == Dir16.WSW) {
						dirSprite = west_south_west.get();
					} else if (direction == Dir16.W) {
						dirSprite = west.get();
					} else if (direction == Dir16.WNW) {
						dirSprite = west_north_west.get();
					} else if (direction == Dir16.NW) {
						dirSprite = north_west.get();
					} else if (direction == Dir16.NNW) {
						dirSprite = north_north_west.get();
					}
					dirSprite.defineSprites(defs.get(defs.size() - 1)::add);
				}
			}
		}
		return defs;
	}

	public void defineSprites(Consumer<? super SpriteDef> consumer, Dir16 direction) {
		defs.get((defs.size() == 1) ? 0 : direction.ordinal()).forEach(consumer);
	}

	public List<SpriteDef> defineSprites(Dir16 direction) {
		List<SpriteDef> ret = new ArrayList<>();
		defineSprites(ret::add, direction);
		return ret;
	}

	public void getDefs(Consumer<ImageDef> register) {
		defs.forEach(l -> l.forEach(register));
	}
}
