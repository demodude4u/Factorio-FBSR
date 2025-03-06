package com.demod.fbsr;

import java.awt.Rectangle;

import com.demod.fbsr.AtlasManager.AtlasRef;

public class ImageDef {

	protected final String path;
	protected final Rectangle source;
	protected final AtlasRef atlasRef;

	protected Rectangle trimmed;

	public ImageDef(String path, Rectangle source) {
		this.path = path;
		this.source = new Rectangle(source);
		atlasRef = new AtlasRef();
	}

	// Links the atlas ref together
	protected ImageDef(ImageDef shared) {
		path = shared.path;
		source = shared.source;
		atlasRef = shared.atlasRef;
		trimmed = shared.trimmed;
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

	public void setTrimmed(Rectangle trimmed) {
		this.trimmed = trimmed;
	}

	public Rectangle getTrimmed() {
		return trimmed;
	}
}