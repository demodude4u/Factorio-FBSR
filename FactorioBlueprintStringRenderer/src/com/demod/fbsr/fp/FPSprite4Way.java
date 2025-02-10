package com.demod.fbsr.fp;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.demod.factorio.FactorioData;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Direction;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Sprite;
import com.google.common.collect.ImmutableList;

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
	}

	public List<Sprite> createSprites(FactorioData data, Direction direction) {
		if (sprite.isPresent()) {
			return sprite.get().createSprites(data);
		} else if (sheets.isPresent()) {
			return sheets.get().stream().map(s -> s.createSprite(data, direction)).collect(Collectors.toList());
		} else if (sheet.isPresent()) {
			return ImmutableList.of(sheet.get().createSprite(data, direction));
		} else {
			FPSprite dirSprite;
			if (direction == Direction.EAST) {
				dirSprite = east.get();
			} else if (direction == Direction.NORTH) {
				dirSprite = north.get();
			} else if (direction == Direction.SOUTH) {
				dirSprite = south.get();
			} else if (direction == Direction.WEST) {
				dirSprite = west.get();
			} else {
				return ImmutableList.of();
			}
			return dirSprite.createSprites(data);
		}
	}

}
