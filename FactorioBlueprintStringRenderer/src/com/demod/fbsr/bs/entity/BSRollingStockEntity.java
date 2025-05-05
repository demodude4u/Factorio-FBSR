package com.demod.fbsr.bs.entity;

import java.util.Optional;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.bs.BSColor;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.legacy.LegacyBlueprintEntity;
import com.demod.fbsr.entity.RollingStockRendering.Placement;

public class BSRollingStockEntity extends BSEntity {
	public final Optional<BSColor> color;

	private Placement actual;

	public BSRollingStockEntity(JSONObject json) {
		super(json);

		color = BSUtils.opt(json, "color", BSColor::new);
	}

	public BSRollingStockEntity(LegacyBlueprintEntity legacy) {
		super(legacy);

		color = BSUtils.opt(legacy.json(), "color", BSColor::new);
	}

	public void setActual(Placement actual) {
		this.actual = actual;
	}

	public Placement getActual() {
		return actual;
	}
}
