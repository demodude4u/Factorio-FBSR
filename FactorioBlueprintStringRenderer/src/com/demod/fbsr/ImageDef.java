package com.demod.fbsr;

import java.awt.Rectangle;

import com.demod.fbsr.AtlasManager.AtlasRef;

public class ImageDef {

	protected final String path;
	protected final Rectangle source;
	protected final AtlasRef atlasRef;

	public ImageDef(String path, Rectangle source) {
		this.path = path;
		this.source = new Rectangle(source);
		atlasRef = new AtlasRef();
	}

	// Links the atlas ref together
	protected ImageDef(ImageDef shared) {
		this.path = shared.path;
		this.source = shared.source;
		atlasRef = shared.atlasRef;
	}

	public AtlasRef getAtlasRef() {
		return atlasRef;
	}

	public String getPath() {
		return path;
	}

	public Rectangle getSource() {
		return source;
	}

}