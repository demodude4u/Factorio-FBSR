package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.ModsProfile;
import com.demod.fbsr.def.SpriteDef;

public class FPSprite extends FPSpriteParameters {

	public final Optional<List<FPSprite>> layers;

	public FPSprite(ModsProfile profile, LuaValue lua) {
		this(profile, lua, true);
	}

	public FPSprite(ModsProfile profile, LuaValue lua, boolean trimmable) {
		super(profile, lua, trimmable);

		layers = FPUtils.optList(lua.get("layers"), l -> new FPSprite(profile, l, trimmable));

	}

	public List<SpriteDef> defineSprites() {
		List<SpriteDef> ret = new ArrayList<>();
		defineSprites(ret::add);
		return ret;
	}

	public void defineSprites(Consumer<? super SpriteDef> consumer) {
		if (layers.isPresent()) {
			for (FPSprite layer : layers.get()) {
				layer.defineSprites(consumer);
			}
			return;
		}

		consumer.accept(super.defineSprite());
	}
}
