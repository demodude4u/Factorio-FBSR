package com.demod.fbsr.entity;

import com.demod.factorio.Utils;
import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.fakelua.LuaValue;

public abstract class TurretRendering extends SimpleEntityRendering {

	private boolean checkState(LuaValue lua, String flag) {
		if (lua.isnil()) {
			return true;
		}
		// XXX
		boolean[] found = { false };
		Utils.forEach(lua.checktable(), l -> {
			if (l.tojstring().equals(flag)) {
				found[0] = true;
			}
		});
		return found[0];
//		LuaValue k = LuaValue.NIL;
//		while (true) {
//			Varargs n = lua.next(k);
//			if ((k = n.arg1()).isnil())
//				break;
//			LuaValue v = n.arg(2);
//			if (v.tojstring().equals(flag)) {
//				return true;
//			}
//		}
//		return false;
	}

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		LuaValue luaBaseVis = lua.get("graphics_set").get("base_visualisation");
		LuaValue luaBaseVisAnimation = luaBaseVis.get("animation");
		if (luaBaseVisAnimation.isnil()) {
			Utils.forEach(luaBaseVis.checktable(), l -> {
				// TODO verify states is correctly used
				LuaValue luaStates = l.get("enabled_states");
				if (checkState(luaStates, "folded")) {
					bind.animation4Way(l.get("animation"));
				}
			});
		} else {
			bind.animation4Way(luaBaseVisAnimation);
		}
		bind.rotatedAnimation8Way(lua.get("folded_animation"));
		bind.circuitConnectorNWay(lua.get("circuit_connector"));
	}

}
