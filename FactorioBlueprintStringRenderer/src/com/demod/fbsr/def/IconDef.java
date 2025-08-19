package com.demod.fbsr.def;

import java.awt.Rectangle;
import java.util.List;

import com.demod.factorio.prototype.DataPrototype;
import com.demod.fbsr.IconLayer;
import com.demod.fbsr.Profile;
import com.demod.fbsr.fp.FPSprite;

public class IconDef extends ImageDef {

	private final int size;
	private final DataPrototype prototype;

	public IconDef(Profile profile, String path, List<IconLayer> layers, int size, DataPrototype prototype) {
		super(profile, path, k -> IconLayer.createIcon(profile.getFactorioManager(), layers, size), new Rectangle(size, size));

		setTrimmable(false);

		this.size = size;
		this.prototype = prototype;
	}

	public int getSize() {
		return size;
	}

	public DataPrototype getPrototype() {
		return prototype;
	}

}
