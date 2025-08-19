package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Layer;
import com.demod.fbsr.Profile;
import com.demod.fbsr.def.LayeredSpriteDef;
import com.demod.fbsr.def.SpriteDef;

public class FPLayeredSprite extends FPSprite {
	public final Optional<Layer> renderLayer;
	public final Optional<List<FPLayeredSprite>> array;

	public FPLayeredSprite(Profile profile, LuaValue lua) {
		super(profile, lua);

		if (!layers.isPresent() && !filename.isPresent()) {
			renderLayer = Optional.empty();
			array = FPUtils.optList(profile, lua, FPLayeredSprite::new);
		} else {
			renderLayer = FPUtils.opt(lua.get("render_layer"), FPUtils::layer);
			array = Optional.empty();
		}
	}

	public List<LayeredSpriteDef> defineLayeredSprites() {
		List<LayeredSpriteDef> ret = new ArrayList<>();
		defineLayeredSprites(ret::add);
		return ret;
	}

	public void defineLayeredSprites(Consumer<? super LayeredSpriteDef> consumer) {
		if (array.isPresent()) {
			for (FPLayeredSprite item : array.get()) {
				item.defineLayeredSprites(consumer);
			}
			return;
		}

		Layer layer = renderLayer.get();
		super.defineSprites(s -> consumer.accept(new LayeredSpriteDef(s, layer)));
	}

	@Override
	@Deprecated
	public List<SpriteDef> defineSprites() {
		throw new InternalError();
	}

	@Override
	@Deprecated
	public void defineSprites(Consumer<? super SpriteDef> consumer) {
		throw new InternalError();
	}

}
