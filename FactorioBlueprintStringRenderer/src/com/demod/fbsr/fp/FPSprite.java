package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.fbsr.FPUtils;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Sprite;

public class FPSprite extends FPSpriteParameters {

	public final Optional<List<FPSprite>> layers;

	public FPSprite(LuaValue lua) {
		super(lua);

		layers = FPUtils.optList(lua.get("layers"), FPSprite::new);

	}

	public List<Sprite> createSprites() {
		List<Sprite> ret = new ArrayList<>();
		createSprites(ret::add);
		return ret;
	}

	public void createSprites(Consumer<Sprite> consumer) {
		if (layers.isPresent()) {
			for (FPSprite layer : layers.get()) {
				layer.createSprites(consumer);
			}
			return;
		}

		Sprite sprite = RenderUtils.createSprite(filename.get(), drawAsShadow, blendMode,
				tint.createColorIgnorePreMultipliedAlpha(), x, y, width, height, shift.x, shift.y, scale);
		consumer.accept(sprite);
	}

}
