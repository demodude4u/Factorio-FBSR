package com.demod.fbsr.bind;

import java.util.OptionalDouble;

import com.demod.fbsr.Layer;

public class BindRotateFrameDef extends BindLayerDef {
	protected OptionalDouble orientation = OptionalDouble.empty();
	protected int frame = 0;

	public BindRotateFrameDef frame(int frame) {
		this.frame = frame;
		return this;
	}

	@Override
	public BindRotateFrameDef layer(Layer layer) {
		super.layer(layer);
		return this;
	}

	public BindRotateFrameDef orientation(double orientation) {
		this.orientation = OptionalDouble.of(orientation);
		return this;
	}
}