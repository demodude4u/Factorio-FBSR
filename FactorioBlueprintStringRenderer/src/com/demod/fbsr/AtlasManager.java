package com.demod.fbsr;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.util.ArrayList;
import java.util.List;

public class AtlasManager {

	public static class Atlas {
		private final BufferedImage bufImage;
		private VolatileImage volImage;

		public Atlas(int width, int height) {
			bufImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		}
	}

	public static class AtlasSprite {
		public AtlasSprite(Atlas atlas, Rectangle rect) {

		}
	}

	private static List<ImageDef> defs = new ArrayList<>();

	private static List<Atlas> atlases = new ArrayList<>();

	public static void registerDef(ImageDef def) {
		defs.add(def);
	}

}
