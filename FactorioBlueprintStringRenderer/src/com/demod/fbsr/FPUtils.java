package com.demod.fbsr;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.Utils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

public final class FPUtils {

	public static double PROJECTION_CONSTANT = 0.7071067811865;

	public static Layer layer(LuaValue lua) {
		return Layer.fromKey(lua.tojstring());
	}

	public static <T> List<T> list(LuaValue lua, Function<LuaValue, T> factory) {
		Builder<T> builder = ImmutableList.builder();
		Utils.forEach(lua, l -> builder.add(factory.apply(l)));
		return builder.build();
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

	// Yo dawg
	public static OptionalInt optInt(LuaValue lua, int defaultValue) {
		if (lua.isnil()) {
			return OptionalInt.empty();
		}
		return OptionalInt.of(lua.optint(defaultValue));
	}

	public static <T> Optional<List<T>> optList(LuaValue lua, Function<LuaValue, T> factory) {
		if (lua.isnil()) {
			return Optional.empty();
		}
		Builder<T> builder = ImmutableList.builder();
		Utils.forEach(lua, l -> builder.add(factory.apply(l)));
		return Optional.of(builder.build());
	}

	public static Optional<String> optString(LuaValue lua) {
		if (lua.isnil()) {
			return Optional.empty();
		}
		return Optional.of(lua.toString());
	}

	public static double projectedOrientation(double orientation) {
		if (orientation == 0 || orientation == 0.25 || orientation == 0.5 || orientation == 0.75)
			return orientation;
		if (orientation < 0.5)
			if (orientation < 0.25) {
				double ratio = Math.tan(orientation * 2 * Math.PI);
				ratio *= PROJECTION_CONSTANT;
				return Math.atan(ratio) / 2.0 / Math.PI;
			} else {
				double ratio = Math.tan((orientation - 0.25) * 2 * Math.PI);
				ratio *= 1 / PROJECTION_CONSTANT;
				return Math.atan(ratio) / 2.0 / Math.PI + 0.25;
			}
		else if (orientation < 0.75) {
			double ratio = Math.tan((0.75 - orientation) * 2 * Math.PI);
			ratio *= 1 / PROJECTION_CONSTANT;
			return 0.75 - Math.atan(ratio) / 2.0 / Math.PI;
		} else {
			double ratio = Math.tan((orientation - 0.75) * 2 * Math.PI);
			ratio *= 1 / PROJECTION_CONSTANT;
			return Math.atan(ratio) / 2.0 / Math.PI + 0.75;
		}
	}

	public static double projectedY(double y) {
		return y * PROJECTION_CONSTANT;
	}

}
