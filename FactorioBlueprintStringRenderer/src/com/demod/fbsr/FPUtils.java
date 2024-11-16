package com.demod.fbsr;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.Utils;

public final class FPUtils {

	public static <T> List<T> list(LuaValue lua, Function<LuaValue, T> factory) {
		List<T> ret = new ArrayList<>();
		Utils.forEach(lua, l -> ret.add(factory.apply(l)));
		return ret;
	}

	public static <T> Optional<T> opt(LuaValue lua, Function<LuaValue, T> factory) {
		if (lua.isnil()) {
			return Optional.empty();
		}
		return Optional.of(factory.apply(lua));
	}

	public static OptionalInt optInt(LuaValue lua) {
		if (lua.isnil()) {
			return OptionalInt.empty();
		}
		return OptionalInt.of(lua.checkint());
	}

	public static <T> Optional<List<T>> optList(LuaValue lua, Function<LuaValue, T> factory) {
		if (lua.isnil()) {
			return Optional.empty();
		}
		List<T> ret = new ArrayList<>();
		Utils.forEach(lua, l -> ret.add(factory.apply(l)));
		return Optional.of(ret);
	}

	public static Optional<String> optString(LuaValue lua) {
		if (lua.isnil()) {
			return Optional.empty();
		}
		return Optional.of(lua.toString());
	}

}
