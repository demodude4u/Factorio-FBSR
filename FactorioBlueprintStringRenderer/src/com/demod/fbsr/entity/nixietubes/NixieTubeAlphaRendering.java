package com.demod.fbsr.entity.nixietubes;

import com.demod.fbsr.EntityType;

@EntityType(value = "lamp", modded = true)
public class NixieTubeAlphaRendering extends NixieTubeBaseRendering {
	public NixieTubeAlphaRendering() {
		super(false, true);
	}
}
