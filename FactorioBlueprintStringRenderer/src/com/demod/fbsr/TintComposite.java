package com.demod.fbsr;

import java.awt.Color;
import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

public class TintComposite implements Composite {
	private final int tintR, tintG, tintB, tintA;

	public TintComposite(Color tint) {
		this.tintR = tint.getRed();
		this.tintG = tint.getGreen();
		this.tintB = tint.getBlue();
		this.tintA = tint.getAlpha();
	}

	@Override
	public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
		return new CompositeContext() {
			@Override
			public void dispose() {
			}

			@Override
			public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
				int width = src.getWidth();
				int height = src.getHeight();
				int[] srcPixels = new int[width * height * 4];
				src.getPixels(0, 0, width, height, srcPixels);

				for (int i = 0; i < srcPixels.length; i += 4) {
					srcPixels[i] = (srcPixels[i] * tintR) / 255;
					srcPixels[i + 1] = (srcPixels[i + 1] * tintG) / 255;
					srcPixels[i + 2] = (srcPixels[i + 2] * tintB) / 255;
					srcPixels[i + 3] = (srcPixels[i + 3] * tintA) / 255;
				}

				dstOut.setPixels(0, 0, width, height, srcPixels);
			}
		};
	}
}