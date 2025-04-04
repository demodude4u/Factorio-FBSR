package com.demod.fbsr.composite;

import java.awt.Color;
import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

public class TintOverlayComposite implements Composite {
	private final float tR, tG, tB, tA;

	public TintOverlayComposite(Color tint) {
		// Store the tint in [0..1] immediately
		this.tR = tint.getRed() / 255f;
		this.tG = tint.getGreen() / 255f;
		this.tB = tint.getBlue() / 255f;
		this.tA = tint.getAlpha() / 255f;
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
				// Dimensions
				int width = Math.min(src.getWidth(), dstIn.getWidth());
				int height = Math.min(src.getHeight(), dstIn.getHeight());

				// Read source pixels (ARGB in [0..255])
				int[] srcPixels = new int[width * height * 4];
				src.getPixels(0, 0, width, height, srcPixels);

				// Read destination pixels
				int[] dstPixels = new int[width * height * 4];
				dstIn.getPixels(0, 0, width, height, dstPixels);

				// Prepare output buffer
				int[] outPixels = new int[width * height * 4];

				// Loop through each pixel
				for (int i = 0; i < srcPixels.length; i += 4) {
					// ---------------------------------------------------------
					// 1) Extract Source RGBA in [0..255], convert to [0..1]
					// ---------------------------------------------------------
					float sr = srcPixels[i] / 255f; // Red
					float sg = srcPixels[i + 1] / 255f; // Green
					float sb = srcPixels[i + 2] / 255f; // Blue
					float sa = srcPixels[i + 3] / 255f; // Alpha

					// If totally transparent source, short-circuit
					if (sa <= 0.00001f) {
						// Just do a standard source-over blend => no change
						// Or you can directly copy dstPixels to outPixels, etc.
						outPixels[i] = dstPixels[i];
						outPixels[i + 1] = dstPixels[i + 1];
						outPixels[i + 2] = dstPixels[i + 2];
						outPixels[i + 3] = dstPixels[i + 3];
						continue;
					}

					// ---------------------------------------------------------
					// 2) Convert source + tint to premultiplied
					// (Factorio’s overlay math expects premultiplied)
					// ---------------------------------------------------------
					// pmSrc: each color channel is multiplied by sa
					float pmSr = sr * sa;
					float pmSg = sg * sa;
					float pmSb = sb * sa;

					// pmTint: each color channel multiplied by tA
					float pmTr = tR * tA;
					float pmTg = tG * tA;
					float pmTb = tB * tA;

					// ---------------------------------------------------------
					// 3) Apply Factorio's overlay formula in premultiplied space
					//
					// alphaOut = sa * tA
					// x = pmSrc.rgb * pmTint.rgb * 2
					// y = alphaOut - 2*(sa - pmSrc.rgb)*(tA - pmTint.rgb)
					//
					// if pmSrc.r < 0.5*sa => out.r = x.r else out.r = y.r
					// (repeat for g,b)
					//
					// ---------------------------------------------------------
					float alphaOverlay = sa * tA;

					// precompute x for each channel
					float xR = pmSr * pmTr * 2.0f;
					float xG = pmSg * pmTg * 2.0f;
					float xB = pmSb * pmTb * 2.0f;

					// precompute y for each channel
					// (alphaOut - 2 * (sa - pmSrc.rgb) * (tA - pmTint.rgb))
					float yR = alphaOverlay - 2.0f * ((sa - pmSr) * (tA - pmTr));
					float yG = alphaOverlay - 2.0f * ((sa - pmSg) * (tA - pmTg));
					float yB = alphaOverlay - 2.0f * ((sa - pmSb) * (tA - pmTb));

					// Decide overlay channel by comparing pmSrc.rgb to 0.5*sa
					float pmOr = (pmSr < 0.5f * sa) ? xR : yR;
					float pmOg = (pmSg < 0.5f * sa) ? xG : yG;
					float pmOb = (pmSb < 0.5f * sa) ? xB : yB;

					// The final overlay alpha in PM space
					// (Factorio sets color.a = color.a * tint.a)
					float pmOa = alphaOverlay;

					// ---------------------------------------------------------
					// 4) Convert overlay result back to NON-premultiplied
					// so we can do standard source-over with the dest
					// ---------------------------------------------------------
					float outSr, outSg, outSb, outSa;
					outSa = pmOa; // same alpha
					if (pmOa > 0.00001f) {
						// unpremultiply each channel
						outSr = pmOr / pmOa;
						outSg = pmOg / pmOa;
						outSb = pmOb / pmOa;
					} else {
						// fully transparent
						outSr = 0f;
						outSg = 0f;
						outSb = 0f;
					}

					// ---------------------------------------------------------
					// 5) Extract destination in [0..1]
					// ---------------------------------------------------------
					float dr = dstPixels[i] / 255f;
					float dg = dstPixels[i + 1] / 255f;
					float db = dstPixels[i + 2] / 255f;
					float da = dstPixels[i + 3] / 255f;

					// ---------------------------------------------------------
					// 6) Source-over alpha blend in NON-premultiplied space
					//
					// alphaFinal = outSa + da*(1 - outSa)
					// colorFinal = (outSr*outSa + dr*da*(1 - outSa)) / alphaFinal
					// ---------------------------------------------------------
					float alphaFinal = outSa + da * (1f - outSa);

					float rr, gg, bb, aa;
					if (alphaFinal > 0.00001f) {
						rr = (outSr * outSa + dr * da * (1f - outSa)) / alphaFinal;
						gg = (outSg * outSa + dg * da * (1f - outSa)) / alphaFinal;
						bb = (outSb * outSa + db * da * (1f - outSa)) / alphaFinal;
					} else {
						rr = gg = bb = 0f;
					}
					aa = alphaFinal;

					// ---------------------------------------------------------
					// 7) Convert final to [0..255] and store
					// ---------------------------------------------------------
					outPixels[i] = Math.round(rr * 255f);
					outPixels[i + 1] = Math.round(gg * 255f);
					outPixels[i + 2] = Math.round(bb * 255f);
					outPixels[i + 3] = Math.round(aa * 255f);
				}

				// Write back
				dstOut.setPixels(0, 0, width, height, outPixels);
			}
		};
	}
}
