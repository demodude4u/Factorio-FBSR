package com.demod.fbsr.fp;

import com.demod.factorio.fakelua.LuaValue;

public class FPVector3D {

	public static FPVector3D opt(LuaValue lua, double x, double y, double z) {
		if (lua.isnil()) {
			return new FPVector3D(x, y, z);
		}
		return new FPVector3D(lua);
	}

	public final double x;
	public final double y;
	public final double z;

	public FPVector3D(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public FPVector3D(LuaValue lua) {
		LuaValue luaX = lua.get("x");
		LuaValue luaY = lua.get("y");
		LuaValue luaZ = lua.get("z");
		if (!luaX.isnil() || !luaY.isnil() || !luaZ.isnil()) {
			x = luaX.checkdouble();
			y = luaY.checkdouble();
			z = luaZ.checkdouble();
		} else {
			x = lua.get(1).checkdouble();
			y = lua.get(2).checkdouble();
			z = lua.get(3).checkdouble();
		}
	}

}
