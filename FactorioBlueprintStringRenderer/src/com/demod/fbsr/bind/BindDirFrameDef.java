package com.demod.fbsr.bind;

import java.util.Optional;

import com.demod.fbsr.Direction;
import com.demod.fbsr.Layer;

public class BindDirFrameDef extends BindLayerDef {
	protected Optional<Direction> direction = Optional.empty();
	protected int frame = 0;

	public BindDirFrameDef direction(Direction direction) {
		this.direction = Optional.of(direction);
		return this;
	}

	public BindDirFrameDef frame(int frame) {
		this.frame = frame;
		return this;
	}

	@Override
	public BindDirFrameDef layer(Layer layer) {
		super.layer(layer);
		return this;
	}
}