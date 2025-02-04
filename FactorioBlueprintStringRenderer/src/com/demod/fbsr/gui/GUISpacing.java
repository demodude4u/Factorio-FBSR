package com.demod.fbsr.gui;

public class GUISpacing {
	public static final GUISpacing NONE = new GUISpacing(0, 0, 0, 0);

	public final int top;
	public final int left;
	public final int bottom;
	public final int right;

	public GUISpacing(int top, int left, int bottom, int right) {
		this.top = top;
		this.left = left;
		this.bottom = bottom;
		this.right = right;
	}

	public GUISpacing add(GUISpacing s) {
		return new GUISpacing(top + s.top, left + s.left, bottom + s.bottom, right + s.bottom);
	}

	public int getHorizontal() {
		return left + right;
	}

	public int getVertical() {
		return top + bottom;
	}

	public GUISpacing subtract(GUISpacing s) {
		return new GUISpacing(top + s.top, left + s.left, bottom + s.bottom, right + s.bottom);
	}
}