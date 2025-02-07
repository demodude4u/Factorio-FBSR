package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.luaj.vm2.LuaValue;

import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Layer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.SpriteWithLayer;

public class FPLayeredSprite extends FPSprite {
	public final Optional<Layer> renderLayer;
	public final Optional<List<FPLayeredSprite>> array;

	public FPLayeredSprite(LuaValue lua) {
		super(lua);

		if (!layers.isPresent() && !filename.isPresent()) {
			renderLayer = Optional.empty();
			array = FPUtils.optList(lua, FPLayeredSprite::new);
		} else {
			renderLayer = FPUtils.opt(lua.get("render_layer"), FPUtils::layer);
			array = Optional.empty();
		}
	}

	public void createSprites(BiConsumer<Layer, Sprite> consumer) {
		if (array.isPresent()) {
			for (FPLayeredSprite item : array.get()) {
				item.createSprites(consumer);
			}
			return;
		}

		super.createSprites(s -> consumer.accept(renderLayer.get(), s));
	}

	public List<SpriteWithLayer> createSpritesWithLayers() {
		List<SpriteWithLayer> ret = new ArrayList<>();
		createSprites((l, s) -> ret.add(new SpriteWithLayer(l, s)));
		return ret;
	}

}
