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

	public TintComposite(int r, int g, int b, int a) {
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
					// 1) Extract the source RGBA
					// ---------------------------------------------------------
					int sr = srcPixels[i]; // Red [0..255]
					int sg = srcPixels[i + 1]; // Green [0..255]
					int sb = srcPixels[i + 2]; // Blue [0..255]
					int sa = srcPixels[i + 3]; // Alpha [0..255]

					// ---------------------------------------------------------
					// 2) Tint the source channels
					// (Multiply each source color/alpha by tint factor)
					// ---------------------------------------------------------
					sr = (sr * tintR) / 255;
					sg = (sg * tintG) / 255;
					sb = (sb * tintB) / 255;
					sa = (sa * tintA) / 255;

					// Convert to float alpha in [0..1]
					float alphaSrc = sa / 255f;

					// ---------------------------------------------------------
					// 3) Extract the destination RGBA
					// ---------------------------------------------------------
					int dr = dstPixels[i];
					int dg = dstPixels[i + 1];
					int db = dstPixels[i + 2];
					int da = dstPixels[i + 3];

					float alphaDst = da / 255f;

					// ---------------------------------------------------------
					// 4) Do source-over alpha compositing in float
					// α_out = α_src + α_dst * (1 - α_src)
					// C_out = (C_src·α_src + C_dst·α_dst·(1 - α_src)) / α_out
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

					// Convert the integer channels to [0..1] floats
					float redSrc = sr / 255f;
					float greenSrc = sg / 255f;
					float blueSrc = sb / 255f;

					float redDst = dr / 255f;
					float greenDst = dg / 255f;
					float blueDst = db / 255f;

					// Source-over formula for each color
					float redOut = (redSrc * alphaSrc + redDst * alphaDst * (1.0f - alphaSrc)) / alphaOut;
					float greenOut = (greenSrc * alphaSrc + greenDst * alphaDst * (1.0f - alphaSrc)) / alphaOut;
					float blueOut = (blueSrc * alphaSrc + blueDst * alphaDst * (1.0f - alphaSrc)) / alphaOut;

					// ---------------------------------------------------------
					// 5) Convert back to int [0..255] and write output
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
