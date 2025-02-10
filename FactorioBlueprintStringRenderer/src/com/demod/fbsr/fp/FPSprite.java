package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.FactorioData;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Sprite;

public class FPSprite extends FPSpriteParameters {

	public final Optional<List<FPSprite>> layers;

	public FPSprite(LuaValue lua) {
		super(lua);

		layers = FPUtils.optList(lua.get("layers"), FPSprite::new);

	}

	public void createSprites(Consumer<Sprite> consumer, FactorioData data) {
		if (layers.isPresent()) {
			for (FPSprite layer : layers.get()) {
				layer.createSprites(consumer, data);
			}
			return;
		}

		consumer.accept(super.createSprite(data));
	}

	public List<Sprite> createSprites(FactorioData data) {
		List<Sprite> ret = new ArrayList<>();
		createSprites(ret::add, data);
		return ret;
	}

}
