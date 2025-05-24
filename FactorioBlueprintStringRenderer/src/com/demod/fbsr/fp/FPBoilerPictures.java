package com.demod.fbsr.fp;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.ModsProfile;

public class FPBoilerPictures {
	public final FPAnimation structure;

	public FPBoilerPictures(ModsProfile profile, LuaValue lua) {
		structure = new FPAnimation(profile, lua.get("structure"));
	}
}