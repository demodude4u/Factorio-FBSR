package com.demod.fbsr;

import java.util.Optional;
import java.util.OptionalDouble;

import com.demod.factorio.prototype.DataPrototype;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.map.MapIcon;
import com.demod.fbsr.map.MapPosition;

public class TagWithQuality {
	private final ImageDef def;
	private final Optional<String> quality;

	public TagWithQuality(ImageDef def, Optional<String> quality) {
		this.def = def;
		this.quality = quality;
	}

	public ImageDef getDef() {
		return def;
	}

	public Optional<String> getQuality() {
		return quality;
	}
}
