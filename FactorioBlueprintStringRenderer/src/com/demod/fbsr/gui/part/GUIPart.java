package com.demod.fbsr.gui.part;

import java.awt.Graphics2D;

import com.demod.fbsr.gui.GUIBox;

public abstract class GUIPart {

	public final GUIBox box;

	public GUIPart(GUIBox box) {
		this.box = box;
	}

	public abstract void render(Graphics2D g);

}
