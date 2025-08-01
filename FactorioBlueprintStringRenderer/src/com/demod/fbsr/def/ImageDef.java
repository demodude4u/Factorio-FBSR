package com.demod.fbsr.def;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.function.Function;

import com.demod.fbsr.Atlas;
import com.demod.fbsr.AtlasPackage;
import com.demod.fbsr.Atlas.AtlasRef;
import com.demod.fbsr.FactorioManager;
import com.demod.fbsr.Profile;

public class ImageDef {

	@FunctionalInterface
	public static interface ImageSheetLoader extends Function<String, BufferedImage> {
	}

	protected final Profile profile;
	protected final String path;
	protected final ImageSheetLoader loader;
	private final boolean shadow;
	protected final Rectangle source;
	protected final AtlasRef atlasRef;

	protected BufferedImage image = null;
	private Rectangle trimmed = null;
	private boolean trimmable = true;

	public ImageDef(Profile profile, String path, ImageSheetLoader loader, Rectangle source) {
		this(profile, path, loader, source, false);
	}

	public ImageDef(Profile profile, String path, ImageSheetLoader loader, Rectangle source, boolean shadow) {
		this.profile = profile;
		this.path = path;
		this.loader = loader;
		this.shadow = shadow;
		this.source = new Rectangle(source);
		atlasRef = new AtlasRef();
	}

	public ImageDef(Profile profile, String path, Rectangle source) {
		this(profile, path, source, false);
	}

	public ImageDef(Profile profile, String path, Rectangle source, boolean shadow) {
		this.profile = profile;
		this.path = path;
		this.loader = profile.getFactorioManager() == null ? null : profile.getFactorioManager()::lookupModImage;
		this.shadow = shadow;
		this.source = new Rectangle(source);
		atlasRef = new AtlasRef();
	}

	// Links the atlas ref together
	protected ImageDef(ImageDef shared) {
		profile = shared.profile;
		path = shared.path;
		loader = shared.loader;
		shadow = shared.shadow;
		source = shared.source;
		atlasRef = shared.atlasRef;
		trimmable = shared.trimmable;
		trimmed = shared.trimmed;
	}

	public void checkValid() {
		if (!atlasRef.isValid()) {
			throw new IllegalStateException("Sprite not on atlas! " + path + " (" + source.x + "," + source.y + ","
					+ source.width + "," + source.height + ")");
		}
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

	public Profile getProfile() {
		return profile;
	}

    public String getModName() {
        String firstSegment = path.split("\\/")[0];
		return firstSegment.substring(2, firstSegment.length() - 2);
    }
}