package com.demod.fbsr.bind;

import com.demod.fbsr.Layer;

public class BindVarFrameDef extends BindFrameDef {
	protected int variation = 0;

	@Override
	public BindVarFrameDef layer(Layer layer) {
		super.layer(layer);
		return this;
	}

	public BindVarFrameDef variation(int variation) {
		this.variation = variation;
		return this;
	}
}