package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.SpriteDef;

public class FPSprite4Way {

	public final Optional<FPSprite> east;
	public final Optional<FPSprite> north;
	public final Optional<FPSprite> south;
	public final Optional<FPSprite> west;
	public final Optional<FPSpriteNWaySheet> sheet;
	// TODO figure out if the sheets are overlapping sprites, or one for each
	// direction
	public final Optional<List<FPSpriteNWaySheet>> sheets;
	public final Optional<FPSprite> sprite;
	private final List<List<SpriteDef>> defs;

	public FPSprite4Way(LuaValue lua) {
		east = FPUtils.opt(lua.get("east"), FPSprite::new);
		north = FPUtils.opt(lua.get("north"), FPSprite::new);
		south = FPUtils.opt(lua.get("south"), FPSprite::new);
		west = FPUtils.opt(lua.get("west"), FPSprite::new);
		sheet = FPSpriteNWaySheet.opt(lua.get("sheet"), 4);
		sheets = FPUtils.optList(lua.get("sheets"), l -> new FPSpriteNWaySheet(l, 4));

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
			for (Direction direction : Direction.cardinals()) {
				defs.add(new ArrayList<>());
				if (sheets.isPresent()) {
					sheets.get().stream().forEach(s -> defs.get(defs.size() - 1).add(s.defineSprite(direction)));
				} else if (sheet.isPresent()) {
					defs.get(defs.size() - 1).add(sheet.get().defineSprite(direction));
				} else {
					FPSprite dirSprite = null;
					if (direction == Direction.EAST) {
						dirSprite = east.get();
					} else if (direction == Direction.NORTH) {
						dirSprite = north.get();
					} else if (direction == Direction.SOUTH) {
						dirSprite = south.get();
					} else if (direction == Direction.WEST) {
						dirSprite = west.get();
					}
					dirSprite.defineSprites(defs.get(defs.size() - 1)::add);
				}
			}
		}
		return defs;
	}

	public void defineSprites(Consumer<? super SpriteDef> consumer, Direction direction) {
		defs.get((defs.size() == 1) ? 0 : direction.cardinal()).forEach(consumer);
	}

	public List<SpriteDef> defineSprites(Direction direction) {
		List<SpriteDef> ret = new ArrayList<>();
		defineSprites(ret::add, direction);
		return ret;
	}

	public List<List<SpriteDef>> getDefs() {
		return defs;
	}
}
