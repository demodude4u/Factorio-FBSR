package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.SpriteDef;

public class FPSprite extends FPSpriteParameters {

	public final Optional<List<FPSprite>> layers;

	public FPSprite(LuaValue lua) {
		super(lua);

		layers = FPUtils.optList(lua.get("layers"), FPSprite::new);

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

	public List<SpriteDef> defineSprites() {
		List<SpriteDef> ret = new ArrayList<>();
		defineSprites(ret::add);
		return ret;
	}
}
