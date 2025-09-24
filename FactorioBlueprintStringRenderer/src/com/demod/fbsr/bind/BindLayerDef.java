package com.demod.fbsr.bind;

import java.util.Optional;
import java.util.function.Consumer;

import com.demod.fbsr.Layer;
import com.demod.fbsr.def.LayeredSpriteDef;
import com.demod.fbsr.def.SpriteDef;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;

public class BindLayerDef extends BindDef {
	protected Layer layer = Layer.OBJECT;
	protected Optional<MapPosition> offset = Optional.empty();

	@Override
	public void defineLayeredSprites(Consumer<LayeredSpriteDef> consumer, MapEntity entity) {
		// XXX I don't like these transient instances
		defineSprites(s -> {
			LayeredSpriteDef sprite = new LayeredSpriteDef(s, layer);
			if (offset.isPresent()) {
				sprite.offset(offset.get());
			}
			consumer.accept(sprite);
		}, entity);
	}

	public void defineSprites(Consumer<SpriteDef> consumer, MapEntity entity) {}

	public BindLayerDef layer(Layer layer) {
		this.layer = layer;
		return this;
	}

	public BindLayerDef offset(MapPosition offset) {
		this.offset = Optional.of(offset);
		return this;
	}

	public BindLayerDef offset(Optional<MapPosition> offset) {
		this.offset = offset;
		return this;
	}
}