package com.demod.fbsr.gui.part;

import java.awt.Graphics2D;
import java.util.Optional;

import com.demod.fbsr.gui.GUIBox;
import com.demod.fbsr.gui.feature.GUISliceFeature;
import com.demod.fbsr.gui.feature.GUIStaticFeature;

public class GUIPanel extends GUIPart {

	public Optional<GUISliceFeature> outer;
	public Optional<GUISliceFeature> inner;
	public Optional<GUIStaticFeature> stat;

	public GUIPanel(GUIBox box, GUISliceFeature inner) {
		super(box);
		this.outer = Optional.empty();
		this.inner = Optional.of(inner);
		this.stat = Optional.empty();
	}

	public GUIPanel(GUIBox box, GUISliceFeature inner, GUISliceFeature outer) {
		super(box);
		this.outer = Optional.of(outer);
		this.inner = Optional.of(inner);
		this.stat = Optional.empty();
	}

	public GUIPanel(GUIBox box, GUIStaticFeature stat) {
		super(box);
		this.outer = Optional.empty();
		this.inner = Optional.empty();
		this.stat = Optional.of(stat);
	}

	public void renderInner(Graphics2D g) {
		if (inner.isPresent()) {
			inner.get().render(g, box);
		}
	}

	public void renderOuter(Graphics2D g) {
		if (outer.isPresent()) {
			outer.get().render(g, box);
		}
	}

	@Override
	public void render(Graphics2D g) {
		if (inner.isPresent()) {
			inner.get().render(g, box);
		}
		if (outer.isPresent()) {
			outer.get().render(g, box);
		}
		if (stat.isPresent()) {
			stat.get().render(g, box);
		}
	}
}
