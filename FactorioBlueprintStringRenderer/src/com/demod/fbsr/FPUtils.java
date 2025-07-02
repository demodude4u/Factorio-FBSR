package com.demod.fbsr;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.demod.factorio.Utils;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.SpriteDef;
import com.demod.fbsr.fp.FPColor;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

public final class FPUtils {

	public static double PROJECTION_CONSTANT = 0.7071067811865;

	public static Direction direction(LuaValue lua) {
		return Direction.values()[lua.toint() / 2];
	}

	public static Layer layer(LuaValue lua) {
		return Layer.fromKey(lua.tojstring());
	}

	public static <T> List<T> list(LuaValue lua, Function<LuaValue, T> factory) {
		if (lua.isnil()) {
			return ImmutableList.of();
		}
		Builder<T> builder = ImmutableList.builder();
		Utils.forEach(lua.checktable(), l -> builder.add(factory.apply(l)));
		return builder.build();
	}

	public static <T> List<T> list(Profile profile, LuaValue lua, BiFunction<Profile, LuaValue, T> factory) {
		if (lua.isnil()) {
			return ImmutableList.of();
		}
		Builder<T> builder = ImmutableList.builder();
		Utils.forEach(lua.checktable(), l -> builder.add(factory.apply(profile, l)));
		return builder.build();
	}

	public static <T> Optional<T> opt(LuaValue lua, Function<LuaValue, T> factory) {
		if (lua.isnil()) {
			return Optional.empty();
		}
		if ((lua.isobject() || lua.isarray()) && lua.length() == 0) {
			return Optional.empty();
		}
		return Optional.of(factory.apply(lua));
	}

	public static <T> Optional<T> opt(Profile profile, LuaValue lua, BiFunction<Profile, LuaValue, T> factory) {
		if (lua.isnil()) {
			return Optional.empty();
		}
		if ((lua.isobject() || lua.isarray()) && lua.length() == 0) {
			return Optional.empty();
		}
		return Optional.of(factory.apply(profile, lua));
	}

	public static Optional<Direction> optDirection(LuaValue lua) {
		if (lua.isnil()) {
			return Optional.empty();
		}
		return Optional.of(Direction.values()[lua.toint() / 2]);
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

	public static Optional<Layer> optLayer(LuaValue lua) {
		if (lua.isnil()) {
			return Optional.empty();
		}
		return Optional.of(Layer.fromKey(lua.tojstring()));
	}

	public static <T> Optional<List<T>> optList(LuaValue lua, Function<LuaValue, T> factory) {
		if (lua.isnil()) {
			return Optional.empty();
		}
		Builder<T> builder = ImmutableList.builder();
		Utils.forEach(lua.checktable(), l -> builder.add(factory.apply(l)));
		return Optional.of(builder.build());
	}

	public static <T> Optional<List<T>> optList(Profile profile, LuaValue lua, BiFunction<Profile, LuaValue, T> factory) {
		if (lua.isnil()) {
			return Optional.empty();
		}
		Builder<T> builder = ImmutableList.builder();
		Utils.forEach(lua.checktable(), l -> builder.add(factory.apply(profile, l)));
		return Optional.of(builder.build());
	}

	public static Optional<String> optString(LuaValue lua) {
		if (lua.isnil()) {
			return Optional.empty();
		}
		return Optional.of(lua.tojstring());
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

	public static BlendMode blendMode(LuaValue lua) {
		if (lua.isnil()) {
			return BlendMode.NORMAL;
		}
		return BlendMode.fromString(lua.tojstring());
	}

	public static Optional<FPColor> tint(LuaValue lua) {
		if (lua.isnil()) {
			return Optional.empty();
		}
		FPColor ret = new FPColor(lua);
		if (ret.r == 1 && ret.g == 1 && ret.b == 1 && ret.a == 1) {
			return Optional.empty();
		}
		return Optional.of(ret);
	}

	public static OptionalDouble optDouble(LuaValue lua) {
		if (lua.isnil()) {
			return OptionalDouble.empty();
		}
		return OptionalDouble.of(lua.todouble());
	}

    public static void verifyNotNull(String label, Object obj) {
        if (obj == null) {
			throw new IllegalArgumentException(label + " is null!");
		}
		
		if (obj instanceof Iterable) {
			int i = 0;
			for (Object def : (Iterable<?>) obj) {
				verifyNotNull(label + "[" + (i++) + "]", def);
			}
		
		} else if (obj.getClass().isArray()) {
			for (int i = 0; i < Array.getLength(obj); i++) {
				verifyNotNull(label + "[" + i + "]", Array.get(obj, i));
			}
		
		} else if (obj instanceof Map) {
			for (Map.Entry<?, ?> entry : ((Map<?, ?>) obj).entrySet()) {
				verifyNotNull(label + "[" + entry.getKey() + "]", entry.getValue());
			}
		}
    }

}
