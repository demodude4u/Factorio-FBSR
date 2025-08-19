package com.demod.fbsr.fp;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Profile;

public class FPUtilitySprites {
	public final FPSprite clock;
	public final FPSprite filterBlacklist;
	public final FPSprite questionmark;
	public final FPSprite gpsMapIcon;
	public final FPSprite tipIcon;
	public final FPSprite customTagIcon;
	public final FPSprite spaceAgeIcon;
	public final FPSprite indicationLine;
	public final FPSprite indicationArrow;

	public FPUtilitySprites(Profile profile, LuaValue lua) {
		clock = new FPSprite(profile, lua.get("clock"), false);
		filterBlacklist = new FPSprite(profile, lua.get("filter_blacklist"), false);
		questionmark = new FPSprite(profile, lua.get("questionmark"), false);
		gpsMapIcon = new FPSprite(profile, lua.get("gps_map_icon"), false);
		tipIcon = new FPSprite(profile, lua.get("tip_icon"), false);
		customTagIcon = new FPSprite(profile, lua.get("custom_tag_icon"), false);
		spaceAgeIcon = new FPSprite(profile, lua.get("space_age_icon"), false);
		indicationLine = new FPSprite(profile, lua.get("indication_line"));
		indicationArrow = new FPSprite(profile, lua.get("indication_arrow"));
	}
}
