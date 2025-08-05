package com.demod.fbsr.entity.nixietubes;

import com.demod.fbsr.EntityType;

@EntityType(value = "lamp", modded = true)
public class NixieTubeSmallRendering extends NixieTubeBaseRendering {
	public NixieTubeSmallRendering() {
		super(true, false);
	}
}
