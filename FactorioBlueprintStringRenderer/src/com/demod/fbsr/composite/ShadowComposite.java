package com.demod.fbsr.composite;

import java.awt.Color;
import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

public class ShadowComposite implements Composite {
	private final int shadowR, shadowG, shadowB;

	public ShadowComposite(Color shadowColor) {
		this.shadowR = shadowColor.getRed();
		this.shadowG = shadowColor.getGreen();
		this.shadowB = shadowColor.getBlue();
	}

	@Override
	public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
		return new CompositeContext() {
			@Override
			public void dispose() {
				// Nothing to clean up
			}

			@Override
			public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
				int width = Math.min(src.getWidth(), dstIn.getWidth());
				int height = Math.min(src.getHeight(), dstIn.getHeight());

				// Read source pixels
				int[] srcPixels = new int[width * height * 4];
				src.getPixels(0, 0, width, height, srcPixels);

				// Read destination pixels
				int[] dstPixels = new int[width * height * 4];
				dstIn.getPixels(0, 0, width, height, dstPixels);

				// Prepare output array
				int[] outPixels = new int[width * height * 4];

				for (int i = 0; i < srcPixels.length; i += 4) {
					// ---------------------------------------------------------
					// 1) Extract the source alpha
					// ---------------------------------------------------------
					int sa = srcPixels[i + 3]; // Alpha [0..255]
					float alphaSrc = sa / 255f;

					// ---------------------------------------------------------
					// 2) Extract the destination RGBA
					// ---------------------------------------------------------
					int dr = dstPixels[i];
					int dg = dstPixels[i + 1];
					int db = dstPixels[i + 2];
					int da = dstPixels[i + 3];
					float alphaDst = da / 255f;

					// ---------------------------------------------------------
					// 3) Do source-over alpha compositing in float
					// ---------------------------------------------------------
					float alphaOut = alphaSrc + alphaDst * (1.0f - alphaSrc);

					// If fully transparent result, just store 0’s
					if (alphaOut <= 0.00001f) {
						outPixels[i] = 0;
						outPixels[i + 1] = 0;
						outPixels[i + 2] = 0;
						outPixels[i + 3] = 0;
						continue;
					}

					// Use the shadow color for output
					float redOut = (shadowR / 255f) * alphaSrc + (dr / 255f) * alphaDst * (1.0f - alphaSrc);
					float greenOut = (shadowG / 255f) * alphaSrc + (dg / 255f) * alphaDst * (1.0f - alphaSrc);
					float blueOut = (shadowB / 255f) * alphaSrc + (db / 255f) * alphaDst * (1.0f - alphaSrc);

					// ---------------------------------------------------------
					// 4) Convert back to int [0..255] and write output
					// ---------------------------------------------------------
					outPixels[i] = Math.round(redOut * 255f);
					outPixels[i + 1] = Math.round(greenOut * 255f);
					outPixels[i + 2] = Math.round(blueOut * 255f);
					outPixels[i + 3] = Math.round(alphaOut * 255f);
				}

				// Store blended pixels back into dstOut
				dstOut.setPixels(0, 0, width, height, outPixels);
			}
		};
	}
}
