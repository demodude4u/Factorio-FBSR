package com.demod.fbsr.bind;

import com.demod.fbsr.Layer;

public class BindVarDef extends BindLayerDef {
	protected int variation = 0;

	@Override
	public BindVarDef layer(Layer layer) {
		super.layer(layer);
		return this;
	}

	public BindVarDef variation(int variation) {
		this.variation = variation;
		return this;
	}
}