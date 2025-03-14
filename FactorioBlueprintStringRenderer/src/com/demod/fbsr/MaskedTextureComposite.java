package com.demod.fbsr;

import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * Draws a mask (the source) where white=fully opaque, black=fully transparent,
 * but uses an external "texture" image to provide the color.
 *
 * The final color = (textureColor * maskAlpha) source-over the existing
 * destination pixel.
 *
 * To use: g.setComposite(new MaskedTextureComposite(texture, texOffsetX,
 * texOffsetY)); g.drawImage(mask, maskX, maskY, null);
 */
public class MaskedTextureComposite implements Composite {
	@Override
	public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
		return new CompositeContext() {

			@Override
			public void dispose() {
				// no resources to release
			}

			@Override
			public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
				// 'src' is the texture pixels,
				// 'dstIn' is the mask

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

						// -----------------------------------------------------
						// (1) Read the mask pixel (the "source")
						// White=opaque => alpha=1, black=transparent => alpha=0
						// -----------------------------------------------------
						int maskR = dstPixels[idx];
						int maskG = dstPixels[idx + 1];
						int maskB = dstPixels[idx + 2];
						int maskA = dstPixels[idx + 3]; // if the mask has alpha

						// We'll interpret the brightness from R/G/B as the "coverage."
						float brightness = (maskR + maskG + maskB) / (3f * 255f);
						float maskAlpha = brightness * (maskA / 255f);

						// Convert to float [0..1]
						float tr = srcPixels[idx] / 255f;
						float tg = srcPixels[idx + 1] / 255f;
						float tb = srcPixels[idx + 2] / 255f;
						float ta = srcPixels[idx + 3] / 255f; // if texture has alpha

						// Multiply texture alpha by maskAlpha => how "visible" it is
						float outA = ta * maskAlpha;

						// Multiply texture color by maskAlpha for that "cutout"
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

				// Write the final output
				dstOut.setPixels(dstIn.getMinX(), dstIn.getMinY(), width, height, outPixels);
			}
		};
	}
}
