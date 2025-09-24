package com.demod.fbsr.bind;

import java.util.Optional;
import java.util.OptionalDouble;

import com.demod.fbsr.Direction;
import com.demod.fbsr.Layer;

public class BindRotateDirFrameDef extends BindLayerDef {
	protected OptionalDouble orientation = OptionalDouble.empty();
	protected int frame = 0;
	protected Optional<Direction> direction = Optional.empty();

	public BindRotateDirFrameDef direction(Direction direction) {
		this.direction = Optional.of(direction);
		return this;
	}

	public BindRotateDirFrameDef frame(int frame) {
		this.frame = frame;
		return this;
	}

	@Override
	public BindRotateDirFrameDef layer(Layer layer) {
		super.layer(layer);
		return this;
	}

	public BindRotateDirFrameDef orientation(double orientation) {
		this.orientation = OptionalDouble.of(orientation);
		return this;
	}
}