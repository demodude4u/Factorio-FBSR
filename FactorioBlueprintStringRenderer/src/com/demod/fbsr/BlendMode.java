package com.demod.fbsr;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum BlendMode {
	NORMAL("normal", AlphaComposite.SrcOver),
	ADDITIVE("additive", new GeneralBlendComposite((src, dst) -> Math.min(255, src + dst))),
	ADDITIVE_SOFT("additive-soft", new GeneralBlendComposite((src, dst) -> (src * (255 - dst) / 255) + dst)),
	MULTIPLICATIVE("multiplicative", new GeneralBlendComposite((src, dst) -> (src * dst) / 255)),
	MULTIPLICATIVE_WITH_ALPHA("multiplicative-with-alpha",
			new GeneralBlendComposite((src, dst, alpha) -> ((src * dst * alpha) / 255) + (dst * (255 - alpha) / 255))),
	OVERWRITE("overwrite", AlphaComposite.Src);

	private static final Map<String, BlendMode> MODE_MAP = Stream.of(values())
			.collect(Collectors.toMap(mode -> mode.name, mode -> mode));

	private final String name;
	private final Composite composite;

	BlendMode(String name, Composite composite) {
		this.name = name;
		this.composite = composite;
	}

	public Composite getComposite() {
		return composite;
	}

	public static BlendMode fromString(String name) {
		return MODE_MAP.getOrDefault(name.toLowerCase(), NORMAL);
	}

	@FunctionalInterface
	private interface BlendFunction {
		int blend(int src, int dst);
	}

	@FunctionalInterface
	private interface BlendFunctionWithAlpha {
		int blend(int src, int dst, int alpha);
	}

	private static class GeneralBlendComposite implements Composite {
		private final BlendFunction blendFunction;
		private final BlendFunctionWithAlpha blendFunctionWithAlpha;

		public GeneralBlendComposite(BlendFunction blendFunction) {
			this.blendFunction = blendFunction;
			this.blendFunctionWithAlpha = null;
		}

		public GeneralBlendComposite(BlendFunctionWithAlpha blendFunctionWithAlpha) {
			this.blendFunction = null;
			this.blendFunctionWithAlpha = blendFunctionWithAlpha;
		}

		@Override
		public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel,
				RenderingHints hints) {
			return new CompositeContext() {
				@Override
				public void dispose() {
				}

				@Override
				public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
					int width = src.getWidth();
					int height = src.getHeight();
					int[] srcPixels = new int[width * height * 4];
					int[] dstPixels = new int[width * height * 4];

					src.getPixels(0, 0, width, height, srcPixels);
					dstIn.getPixels(0, 0, width, height, dstPixels);

					for (int i = 0; i < srcPixels.length; i += 4) {
						int srcR = srcPixels[i], srcG = srcPixels[i + 1], srcB = srcPixels[i + 2],
								srcA = srcPixels[i + 3];
						int dstR = dstPixels[i], dstG = dstPixels[i + 1], dstB = dstPixels[i + 2],
								dstA = dstPixels[i + 3];

						if (blendFunction != null) {
							dstPixels[i] = blendFunction.blend(srcR, dstR);
							dstPixels[i + 1] = blendFunction.blend(srcG, dstG);
							dstPixels[i + 2] = blendFunction.blend(srcB, dstB);
						} else if (blendFunctionWithAlpha != null) {
							dstPixels[i] = blendFunctionWithAlpha.blend(srcR, dstR, srcA);
							dstPixels[i + 1] = blendFunctionWithAlpha.blend(srcG, dstG, srcA);
							dstPixels[i + 2] = blendFunctionWithAlpha.blend(srcB, dstB, srcA);
						}

						dstPixels[i + 3] = Math.max(srcA, dstA); // Preserve max alpha
					}

					dstOut.setPixels(0, 0, width, height, dstPixels);
				}
			};
		}
	}
}
