package com.demod.fbsr.entity.nixietubes;

import com.demod.fbsr.EntityType;

@EntityType(value = "lamp", modded = true)
public class NixieTubeRendering extends NixieTubeBaseRendering {
	public NixieTubeRendering() {
		super(false, false);
	}
}
