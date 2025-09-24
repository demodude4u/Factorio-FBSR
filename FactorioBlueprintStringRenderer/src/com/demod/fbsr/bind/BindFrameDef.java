package com.demod.fbsr.bind;

import com.demod.fbsr.Layer;

public class BindFrameDef extends BindLayerDef {
	protected int frame = 0;

	public BindFrameDef frame(int frame) {
		this.frame = frame;
		return this;
	}

	@Override
	public BindFrameDef layer(Layer layer) {
		super.layer(layer);
		return this;
	}
}