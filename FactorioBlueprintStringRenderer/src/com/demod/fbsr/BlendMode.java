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
	NORMAL("normal", AlphaComposite.SrcOver), //
	ADDITIVE("additive", new GeneralBlendComposite((src, dst) -> {
		for (int i = 0; i < src.length; i += 4) {
			int Sa = src[i + 0];
			int Sr = src[i + 1];
			int Sg = src[i + 2];
			int Sb = src[i + 3];
			int SpR = (Sr * Sa + 127) / 255;
			int SpG = (Sg * Sa + 127) / 255;
			int SpB = (Sb * Sa + 127) / 255;
			int Da = dst[i + 0];
			int Dr = dst[i + 1];
			int Dg = dst[i + 2];
			int Db = dst[i + 3];

			dst[i + 0] = Da;
			dst[i + 1] = Math.min(SpR + Dr, 255);
			dst[i + 2] = Math.min(SpG + Dg, 255);
			dst[i + 3] = Math.min(SpB + Db, 255);
		}
	})), //

	ADDITIVE_SOFT("additive-soft", new GeneralBlendComposite((src, dst) -> {
		for (int i = 0; i < src.length; i += 4) {
			int Sa = src[i + 0];
			int Sr = src[i + 1];
			int Sg = src[i + 2];
			int Sb = src[i + 3];
			int SpR = (Sr * Sa + 127) / 255;
			int SpG = (Sg * Sa + 127) / 255;
			int SpB = (Sb * Sa + 127) / 255;
			int Da = dst[i + 0];
			int Dr = dst[i + 1];
			int Dg = dst[i + 2];
			int Db = dst[i + 3];

			dst[i + 0] = Da;
			dst[i + 1] = ((SpR * (255 - Dr) + Dr * 255) + 127) / 255;
			dst[i + 2] = ((SpG * (255 - Dg) + Dg * 255) + 127) / 255;
			dst[i + 3] = ((SpB * (255 - Db) + Db * 255) + 127) / 255;
		}
	})), //
	MULTIPLICATIVE("multiplicative", new GeneralBlendComposite((src, dst) -> {
		for (int i = 0; i < src.length; i += 4) {
			int Sa = src[i + 0];
			int Sr = src[i + 1];
			int Sg = src[i + 2];
			int Sb = src[i + 3];
			int SpR = (Sr * Sa + 127) / 255;
			int SpG = (Sg * Sa + 127) / 255;
			int SpB = (Sb * Sa + 127) / 255;
			int Da = dst[i + 0];
			int Dr = dst[i + 1];
			int Dg = dst[i + 2];
			int Db = dst[i + 3];

			dst[i + 0] = Da;
			dst[i + 1] = (SpR * Dr + 127) / 255;
			dst[i + 2] = (SpG * Dg + 127) / 255;
			dst[i + 3] = (SpB * Db + 127) / 255;
		}
	})), //
	MULTIPLICATIVE_WITH_ALPHA("multiplicative-with-alpha", new GeneralBlendComposite((src, dst) -> {
		for (int i = 0; i < src.length; i += 4) {
			int Sa = src[i + 0];
			int Sr = src[i + 1];
			int Sg = src[i + 2];
			int Sb = src[i + 3];
			int SpR = (Sr * Sa + 127) / 255;
			int SpG = (Sg * Sa + 127) / 255;
			int SpB = (Sb * Sa + 127) / 255;
			int Da = dst[i + 0];
			int Dr = dst[i + 1];
			int Dg = dst[i + 2];
			int Db = dst[i + 3];

			int invSa = 255 - Sa;
			dst[i + 0] = Da;
			dst[i + 1] = ((SpR * Dr + Dr * invSa) + 127) / 255;
			dst[i + 2] = ((SpG * Dg + Dg * invSa) + 127) / 255;
			dst[i + 3] = ((SpB * Db + Db * invSa) + 127) / 255;
		}
	})), //
	OVERWRITE("overwrite", AlphaComposite.Src);//

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
		void blend(int[] src, int[] dst);
	}

	private static class GeneralBlendComposite implements Composite {
		private final BlendFunction blendFunction;

		public GeneralBlendComposite(BlendFunction blendFunction) {
			this.blendFunction = blendFunction;
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

					blendFunction.blend(srcPixels, dstPixels);

					dstOut.setPixels(0, 0, width, height, dstPixels);
				}
			};
		}
	}
}
