package com.demod.fbsr.bind;

import java.util.OptionalDouble;

import com.demod.fbsr.Layer;

public class BindRotateDef extends BindLayerDef {
	protected OptionalDouble orientation = OptionalDouble.empty();

	@Override
	public BindRotateDef layer(Layer layer) {
		super.layer(layer);
		return this;
	}

	public BindRotateDef orientation(double orientation) {
		this.orientation = OptionalDouble.of(orientation);
		return this;
	}
}