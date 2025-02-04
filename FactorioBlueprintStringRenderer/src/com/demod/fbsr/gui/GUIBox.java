package com.demod.fbsr.gui;

public class GUIBox {
	public final int x;
	public final int y;
	public final int width;
	public final int height;

	public GUIBox(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public GUIBox cutBottom(int bottom) {
		return new GUIBox(x, y + (height - bottom), width, bottom);
	}

	public GUIBox cutLeft(int left) {
		return new GUIBox(x, y, left, height);
	}

	public GUIBox cutRight(int right) {
		return new GUIBox(x + (width - right), y, right, height);
	}

	public GUIBox cutTop(int top) {
		return new GUIBox(x, y, width, top);
	}

	public GUIBox expand(GUISpacing s) {
		return new GUIBox(x - s.left, y - s.top, width + s.left + s.right, height + s.top + s.bottom);
	}

	public GUIBox expand(int top, int left, int bottom, int right) {
		return new GUIBox(x - left, y - top, width + left + right, height + top + bottom);
	}

	public GUIBox expandBottom(int bottom) {
		return new GUIBox(x, y, width, height + bottom);
	}

	public GUIBox expandLeft(int left) {
		return new GUIBox(x - left, y, width + left, height);
	}

	public GUIBox expandRight(int right) {
		return new GUIBox(x, y, width + right, height);
	}

	public GUIBox expandTop(int top) {
		return new GUIBox(x, y - top, width, height + top);
	}

	public GUIBox indexed(int row, int col) {
		return new GUIBox(x + width * col, y + height * row, width, height);
	}

	public GUIBox shrink(GUISpacing s) {
		return new GUIBox(x + s.left, y + s.top, width - s.left - s.right, height - s.top - s.bottom);
	}

	public GUIBox shrink(int top, int left, int bottom, int right) {
		return new GUIBox(x + left, y + top, width - left - right, height - top - bottom);
	}

	public GUIBox shrinkBottom(int bottom) {
		return new GUIBox(x, y, width, height - bottom);
	}

	public GUIBox shrinkLeft(int left) {
		return new GUIBox(x + left, y, width - left, height);
	}

	public GUIBox shrinkRight(int right) {
		return new GUIBox(x, y, width - right, height);
	}

	public GUIBox shrinkTop(int top) {
		return new GUIBox(x, y + top, width, height - top);
	}
}