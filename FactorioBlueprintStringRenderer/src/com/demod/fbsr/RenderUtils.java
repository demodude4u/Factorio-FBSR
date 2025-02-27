package com.demod.fbsr;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.factorio.FactorioData;
import com.demod.factorio.Utils;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.map.MapRect;
import com.demod.fbsr.map.MapSprite;

public final class RenderUtils {

	public static final BufferedImage EMPTY_IMAGE = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
	private static final Logger LOGGER = LoggerFactory.getLogger(RenderUtils.class);

	static {
		EMPTY_IMAGE.setRGB(0, 0, 0x00000000);
	}

	private static final DecimalFormat DECIMAL_FORMAT_2_PLACES = new DecimalFormat("#,##0.##");

	public static MapSprite createSprite(FactorioData data, Layer layer, SpriteDef spriteDef) {

		BufferedImage image = data.getModImage(spriteDef.getPath());

		if (!spriteDef.getBlendMode().equals("normal")) { // FIXME blending will take effort
			image = RenderUtils.EMPTY_IMAGE;
		}

		if (!spriteDef.getTint().equals(Color.white)) {
			image = Utils.tintImage(image, spriteDef.getTint());
		}

		return new MapSprite(layer, EMPTY_IMAGE, spriteDef.getSource(), spriteDef.getBounds());
	}

	public static String fmtDouble(double value) {
		if (value == (long) value) {
			return String.format("%d", (long) value);
		} else {
			return Double.toString(value);// String.format("%f", value);
		}
	}

	public static String fmtDouble2(double value) {
		return DECIMAL_FORMAT_2_PLACES.format(value);
	}

	public static String fmtItemQuantity(double amount) {
		String amountStr;
		if (amount < 10000) {
			amountStr = fmtDouble(Math.ceil(amount));
		} else if (amount < 1000000) {
			amountStr = fmtDouble(Math.ceil(amount / 1000)) + "k";
		} else {
			amountStr = fmtDouble(Math.ceil(amount / 1000000)) + "M";
		}
		return amountStr;
	}

	public static Color getAverageColor(BufferedImage image) {
		int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
		float sumR = 0, sumG = 0, sumB = 0, sumA = 0;
		for (int pixel : pixels) {
			float a = (pixel >> 24) & 0xFF;
			float f = a / 255;
			sumA += a;
			sumR += ((pixel >> 16) & 0xFF) * f;
			sumG += ((pixel >> 8) & 0xFF) * f;
			sumB += ((pixel) & 0xFF) * f;
		}
		return new Color(sumR / sumA, sumG / sumA, sumB / sumA);
	}

	public static Color getUnknownColor(String name) {
		Random random = new Random(name.hashCode());
		return RenderUtils.withAlpha(Color.getHSBColor(random.nextFloat(), 0.6f, 0.4f), 128);
	}

	public static float getUnknownTextOffset(String name) {
		Random random = new Random(name.hashCode());
		return random.nextFloat();
	}

	public static Color parseColor(JSONObject json) {
		return new Color((float) json.getDouble("r"), (float) json.getDouble("g"), (float) json.getDouble("b"),
				(float) json.getDouble("a"));
	}

	public static <T> T pickDirectional(List<T> list, BSEntity entity) {
		switch (list.size()) {
		case 1:
			return list.get(0);
		case 4:
			return list.get(entity.direction.cardinal());
		case 8:
			return list.get(entity.direction.ordinal());
		case 16:
			return list.get(entity.directionRaw);
		}
		return null;// XXX should I do something?
	}

	// Useful for BS and FP objects
	public static void printObjectTree(Object obj) {
		printObjectTree(obj, "");
	}

	private static void printObjectTree(Object obj, String prefix) {
		if (obj == null) {
			LOGGER.info("{}null", prefix);
			return;
		}
		Class<?> clazz = obj.getClass();

		if (obj instanceof Collection) {
			LOGGER.info("{}(Collection):", prefix);
			for (Object item : (Collection<?>) obj)
				printObjectTree(item, prefix + "|  ");
		} else if (obj instanceof Optional) {
			Optional<?> optional = (Optional<?>) obj;
			if (optional.isPresent()) {
				LOGGER.info("{}(Optional Present):", prefix);
				printObjectTree(optional.get(), prefix + "|  ");
			} else {
				LOGGER.info("{}(Optional Empty)", prefix);
			}
		} else if (clazz.isPrimitive() || obj instanceof String || obj instanceof Number || obj instanceof Boolean) {
			LOGGER.info("{}{}", prefix, obj.toString());
		} else {
			LOGGER.info("{}{}:", prefix, clazz.getName());
			for (Field field : clazz.getDeclaredFields()) {
				if (java.lang.reflect.Modifier.isPublic(field.getModifiers())
						&& java.lang.reflect.Modifier.isFinal(field.getModifiers())) {
					try {
						field.setAccessible(true);
						Object fieldValue = field.get(obj);
						if (fieldValue == null || fieldValue instanceof String || fieldValue instanceof Number
								|| fieldValue instanceof Boolean || fieldValue.getClass().isPrimitive()) {
							LOGGER.info("{}|  {}: {}", prefix, field.getName(), fieldValue);
						} else {
							LOGGER.info("{}|  {}: ({})", prefix, field.getName(), fieldValue.getClass().getName());
							printObjectTree(fieldValue, prefix + "|  ");
						}
					} catch (IllegalAccessException e) {
						LOGGER.info("{}|  {}: [Error accessing field]", prefix, field.getName());
					}
				}
			}
		}
	}

	public static BufferedImage scaleImage(BufferedImage image, int width, int height) {
		BufferedImage ret = new BufferedImage(width, height, image.getType());
		Graphics2D g = ret.createGraphics();
		g.drawImage(image, 0, 0, width, height, null);
		g.dispose();
		return ret;
	}

	public static Color withAlpha(Color color, int alpha) {
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}

	private RenderUtils() {
	}

}
