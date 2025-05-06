package com.demod.fbsr.fp;

import com.demod.factorio.fakelua.LuaValue;

public class FPUtilitySprites {
	public final FPSprite clock;
	public final FPSprite filterBlacklist;
	public final FPSprite questionmark;
	public final FPSprite gpsMapIcon;
	public final FPSprite tipIcon;
	public final FPSprite customTagIcon;
	public final FPSprite spaceAgeIcon;

	public FPUtilitySprites(LuaValue lua) {
		clock = new FPSprite(lua.get("clock"), false);
		filterBlacklist = new FPSprite(lua.get("filter_blacklist"), false);
		questionmark = new FPSprite(lua.get("questionmark"), false);
		gpsMapIcon = new FPSprite(lua.get("gps_map_icon"), false);
		tipIcon = new FPSprite(lua.get("tip_icon"), false);
		customTagIcon = new FPSprite(lua.get("custom_tag_icon"), false);
		spaceAgeIcon = new FPSprite(lua.get("space_age_icon"), false);
	}
}
