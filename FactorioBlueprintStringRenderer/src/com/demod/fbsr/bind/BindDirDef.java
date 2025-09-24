package com.demod.fbsr.bind;

import java.util.Optional;

import com.demod.fbsr.Direction;
import com.demod.fbsr.Layer;

public class BindDirDef extends BindLayerDef {
	protected Optional<Direction> direction = Optional.empty();

	public BindDirDef direction(Direction direction) {
		this.direction = Optional.of(direction);
		return this;
	}

	@Override
	public BindDirDef layer(Layer layer) {
		super.layer(layer);
		return this;
	}
}