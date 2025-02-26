package com.demod.fbsr;

import java.awt.Rectangle;

public class ImageDef {

	protected final String path;
	protected final Rectangle source;

	public ImageDef(String path, Rectangle source) {
		super();
		this.path = path;
		this.source = source;
	}

	public String getPath() {
		return path;
	}

	public Rectangle getSource() {
		return source;
	}

}