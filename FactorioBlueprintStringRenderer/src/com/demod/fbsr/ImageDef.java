package com.demod.fbsr;

import java.awt.Rectangle;
import java.util.Optional;

import com.demod.fbsr.AtlasManager.Atlas;
import com.demod.fbsr.AtlasManager.AtlasRef;

public class ImageDef {

	protected final String path;
	protected final Rectangle source;

	protected AtlasRef atlasRef = null;

	public ImageDef(String path, Rectangle source) {
		super();
		this.path = path;
		this.source = source;
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

	public void setAtlasRef(AtlasRef atlasRef) {
		this.atlasRef = atlasRef;
	}

}