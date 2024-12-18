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
}