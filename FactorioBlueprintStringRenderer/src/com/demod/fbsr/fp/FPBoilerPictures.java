package com.demod.fbsr.fp;

import com.demod.factorio.fakelua.LuaValue;

public class FPBoilerPictures {
	public final FPAnimation structure;

	public FPBoilerPictures(LuaValue lua) {
		structure = new FPAnimation(lua.get("structure"));
	}
}