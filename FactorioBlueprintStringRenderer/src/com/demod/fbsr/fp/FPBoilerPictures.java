package com.demod.fbsr.fp;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.Profile;

public class FPBoilerPictures {
	public final FPAnimation structure;

	public FPBoilerPictures(Profile profile, LuaValue lua) {
		structure = new FPAnimation(profile, lua.get("structure"));
	}
}