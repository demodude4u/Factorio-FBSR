package com.demod.fbsr;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.function.Function;

import com.demod.fbsr.AtlasManager.AtlasRef;

public class ImageDef {

	@FunctionalInterface
	public static interface ImageSheetLoader extends Function<String, BufferedImage> {
	}

	protected final String path;
	protected final ImageSheetLoader loader;
	protected final Rectangle source;
	protected final AtlasRef atlasRef;

	protected BufferedImage image = null;
	protected Rectangle trimmed = null;

	public ImageDef(String path, ImageSheetLoader loader, Rectangle source) {
		this.path = path;
		this.loader = loader;
		this.source = new Rectangle(source);
		atlasRef = new AtlasRef();
	}

	public ImageDef(String path, Rectangle source) {
		this.path = path;
		this.loader = FactorioManager::lookupModImage;
		this.source = new Rectangle(source);
		atlasRef = new AtlasRef();
	}

	// Links the atlas ref together
	protected ImageDef(ImageDef shared) {
		path = shared.path;
		loader = shared.loader;
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

	public ImageSheetLoader getLoader() {
		return loader;
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