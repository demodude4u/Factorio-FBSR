package com.demod.fbsr.gui;

public class GUISize {
	public final int width;
	public final int height;

	public GUISize(int width, int height) {
		this.width = width;
		this.height = height;

	}

	public GUIBox toBox(int x, int y) {
		return new GUIBox(x, y, width, height);
	}
}
