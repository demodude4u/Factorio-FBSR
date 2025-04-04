package com.demod.fbsr;

import java.util.Optional;
import java.util.OptionalDouble;

import com.demod.fbsr.def.IconDef;
import com.demod.fbsr.map.MapIcon;
import com.demod.fbsr.map.MapPosition;

public class IconDefWithQuality {
	private final IconDef def;
	private final Optional<String> quality;

	public IconDefWithQuality(IconDef def, Optional<String> quality) {
		this.def = def;
		this.quality = quality;
	}

	public MapIcon createMapIcon(MapPosition position, double size, OptionalDouble border, boolean above) {
		Optional<String> quality = this.quality.filter(s -> !s.equals("normal"));
		return new MapIcon(position, def, size, border, above, quality);
	}

	public IconDef getDef() {
		return def;
	}

	public Optional<String> getQuality() {
		return quality;
	}
}
