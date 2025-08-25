package com.demod.fbsr.composite;

import java.awt.Color;
import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * Applies a tint to the "texture" (src), then uses the "mask" (dstIn) to
 * control visibility (white=opaque, black=transparent). The result is
 * written to dstOut without additional blending over the destination.
 *
 * Usage pattern mirrors MaskedTextureComposite's current behavior:
 * - src: texture pixels
 * - dstIn: mask pixels
 */
public class TintMaskedTextureComposite implements Composite {
	private final int tintR, tintG, tintB, tintA;

	public TintMaskedTextureComposite(Color tint) {
		this.tintR = tint.getRed();
		this.tintG = tint.getGreen();
		this.tintB = tint.getBlue();
		this.tintA = tint.getAlpha();
	}

	public TintMaskedTextureComposite(int r, int g, int b, int a) {
		this.tintR = r;
		this.tintG = g;
		this.tintB = b;
		this.tintA = a;
	}

	@Override
	public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
		return new CompositeContext() {
			@Override
			public void dispose() {
				// no resources to release
			}

			@Override
			public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
				// 'src' is the texture pixels
				// 'dstIn' is the mask pixels
				int width = Math.min(src.getWidth(), dstIn.getWidth());
				int height = Math.min(src.getHeight(), dstIn.getHeight());

				int[] srcPixels = new int[width * height * 4];
				int[] dstPixels = new int[width * height * 4];
				int[] outPixels = new int[width * height * 4];

				src.getPixels(src.getMinX(), src.getMinY(), width, height, srcPixels);
				dstIn.getPixels(dstIn.getMinX(), dstIn.getMinY(), width, height, dstPixels);

				int idx = 0;
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						// Read mask pixel (grayscale brightness + mask alpha)
						int maskR = dstPixels[idx];
						int maskG = dstPixels[idx + 1];
						int maskB = dstPixels[idx + 2];
						int maskA = dstPixels[idx + 3]; // if mask has alpha

						float brightness = (maskR + maskG + maskB) / (3f * 255f);
						float maskAlpha = brightness * (maskA / 255f);

						// Read texture pixel and apply tint
						int trI = (srcPixels[idx] * tintR) / 255;
						int tgI = (srcPixels[idx + 1] * tintG) / 255;
						int tbI = (srcPixels[idx + 2] * tintB) / 255;
						int taI = (srcPixels[idx + 3] * tintA) / 255;

						float tr = trI / 255f;
						float tg = tgI / 255f;
						float tb = tbI / 255f;
						float ta = taI / 255f;

						// Apply mask alpha to both color and alpha
						float outA = ta * maskAlpha;
						float outR = tr * maskAlpha;
						float outG = tg * maskAlpha;
						float outB = tb * maskAlpha;

						outPixels[idx] = Math.round(outR * 255f);
						outPixels[idx + 1] = Math.round(outG * 255f);
						outPixels[idx + 2] = Math.round(outB * 255f);
						outPixels[idx + 3] = Math.round(outA * 255f);

						idx += 4;
					}
				}

				dstOut.setPixels(dstIn.getMinX(), dstIn.getMinY(), width, height, outPixels);
			}
		};
	}
}
