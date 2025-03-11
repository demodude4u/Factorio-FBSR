package com.demod.fbsr;

import java.awt.Point;
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
	private final boolean shadow;
	protected final Rectangle source;
	protected final AtlasRef atlasRef;

	protected BufferedImage image = null;
	private Rectangle trimmed = null;
	private boolean trimmable = true;

	public ImageDef(String path, ImageSheetLoader loader, Rectangle source) {
		this(path, loader, source, false);
	}

	public ImageDef(String path, ImageSheetLoader loader, Rectangle source, boolean shadow) {
		this.path = path;
		this.loader = loader;
		this.shadow = shadow;
		this.source = new Rectangle(source);
		atlasRef = new AtlasRef();
	}

	public ImageDef(String path, Rectangle source) {
		this(path, source, false);
	}

	public ImageDef(String path, Rectangle source, boolean shadow) {
		this.path = path;
		this.loader = FactorioManager::lookupModImage;
		this.shadow = shadow;
		this.source = new Rectangle(source);
		atlasRef = new AtlasRef();
	}

	// Links the atlas ref together
	protected ImageDef(ImageDef shared) {
		path = shared.path;
		loader = shared.loader;
		shadow = shared.shadow;
		source = shared.source;
		atlasRef = shared.atlasRef;
		trimmed = shared.trimmed;
	}

	public boolean isShadow() {
		return shadow;
	}

	public boolean isTrimmable() {
		return trimmable;
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
		if (trimmed == null) {
			Point trim = atlasRef.getTrim();
			Rectangle rect = atlasRef.getRect();
			trimmed = new Rectangle(source.x + trim.x, source.y + trim.y, rect.width, rect.height);
		}
		return trimmed;
	}

	public void setTrimmable(boolean trimmable) {
		this.trimmable = trimmable;
	}
}