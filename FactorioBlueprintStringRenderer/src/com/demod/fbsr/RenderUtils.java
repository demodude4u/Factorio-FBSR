package com.demod.fbsr;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.factorio.FactorioData;
import com.demod.factorio.Utils;
import com.demod.factorio.prototype.ItemPrototype;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.BSItemStack;
import com.demod.fbsr.map.MapRect3D;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;

public final class RenderUtils {

	public static final BufferedImage EMPTY_IMAGE = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
	private static final Logger LOGGER = LoggerFactory.getLogger(RenderUtils.class);

	static {
		EMPTY_IMAGE.setRGB(0, 0, 0x00000000);
	}

	private static final DecimalFormat DECIMAL_FORMAT_2_PLACES = new DecimalFormat("#,##0.##");

	public static Sprite createSprite(FactorioData data, String filename, Layer layer, String blendMode, Color tint,
			int srcX, int srcY, int srcWidth, int srcHeight, double dstX, double dstY, double dstScale) {

		Sprite ret = new Sprite();
		ret.image = data.getModImage(filename);
		ret.layer = layer;

		if (!blendMode.equals("normal")) { // FIXME blending will take effort
			ret.image = RenderUtils.EMPTY_IMAGE;
		}

		if (!tint.equals(Color.white)) {
			ret.image = Utils.tintImage(ret.image, tint);
		}

		double scaledWidth = dstScale * srcWidth / FBSR.TILE_SIZE;
		double scaledHeight = dstScale * srcHeight / FBSR.TILE_SIZE;
		ret.source = new Rectangle(srcX, srcY, srcWidth, srcHeight);
		ret.bounds = new Rectangle2D.Double(dstX - scaledWidth / 2.0, dstY - scaledHeight / 2.0, scaledWidth,
				scaledHeight);

		return ret;
	}

	public static Renderer createWireRenderer(Point2D.Double p1, Point2D.Double p2, Color color, Point2D.Double shadow1,
			Point2D.Double shadow2) {
		Rectangle2D.Double bounds2D = new Rectangle2D.Double();
		bounds2D.setFrameFromDiagonal(p1, p2);
		MapRect3D bounds = new MapRect3D(bounds2D, 0);

		return new EntityRenderer(Layer.WIRE, bounds, true) {
			final double drop = 0.6;

			@Override
			public void render(Graphics2D g) {
				Stroke ps = g.getStroke();
				g.setStroke(new BasicStroke(1f / 32f));
				g.setColor(color);

				Path2D.Double path = new Path2D.Double();
				path.moveTo(p1.x, p1.y);
				Point2D.Double mid = new Point2D.Double((p1.x + p2.x) / 2, (p1.y + p2.y) / 2 + drop);
				path.curveTo(mid.x, mid.y, mid.x, mid.y, p2.x, p2.y);
				g.draw(path);

				g.setStroke(ps);
			}

			@Override
			public void renderShadows(Graphics2D g) throws Exception {
				Stroke ps = g.getStroke();
				g.setStroke(new BasicStroke(1f / 48f));
				g.setColor(Color.black);

				Path2D.Double path = new Path2D.Double();
				path.moveTo(shadow1.x, shadow1.y);
				Point2D.Double mid = new Point2D.Double((shadow1.x + shadow2.x) / 2 - drop,
						(shadow1.y + shadow2.y) / 2);
				path.curveTo(mid.x, mid.y, mid.x, mid.y, shadow2.x, shadow2.y);
				g.draw(path);

				g.setStroke(ps);
			}
		};
	}

	public static void drawImageInBounds(BufferedImage image, Rectangle source, Rectangle2D.Double bounds,
			Graphics2D g) {
		AffineTransform pat = g.getTransform();
		g.translate(bounds.x, bounds.y);
		g.scale(bounds.width, bounds.height);
		g.drawImage(image, 0, 0, 1, 1, source.x, source.y, source.x + source.width, source.y + source.height, null);
		g.setTransform(pat);
	}

	public static Renderer drawRotatedString(Layer layer, Point2D.Double position, double angle, Color color,
			String string) {
		return new Renderer(layer, position, true) {
			@Override
			public void render(Graphics2D g) {
				AffineTransform pat = g.getTransform();

				g.setFont(new Font("Monospaced", Font.BOLD, 1).deriveFont(0.4f));
				float textX = (float) position.x;
				float textY = (float) position.y;

				g.translate(textX, textY);
				g.rotate(angle);

				g.setColor(Color.darkGray);
				g.drawString(string, 0.05f, 0.05f);
				g.setColor(color);
				g.drawString(string, 0f, 0f);

				g.setTransform(pat);
			}
		};
	}

	public static void drawSprite(Sprite sprite, Graphics2D g) {
		drawImageInBounds(sprite.image, sprite.source, sprite.bounds, g);
	}

	public static Renderer drawString(Layer layer, Point2D.Double position, Color color, String string) {
		return new Renderer(layer, position, true) {
			@Override
			public void render(Graphics2D g) {
				g.setFont(new Font("Monospaced", Font.BOLD, 1).deriveFont(0.4f));
				float textX = (float) position.getX();
				float textY = (float) position.getY();
				g.setColor(Color.darkGray);
				g.drawString(string, textX + 0.05f, textY + 0.05f);
				g.setColor(color);
				g.drawString(string, textX, textY);
			}
		};
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

	public static Multiset<String> getModules(BSEntity entity) {

		Multiset<String> modules = LinkedHashMultiset.create();

		for (BSItemStack itemStack : entity.items) {
			String itemName = itemStack.id.name;
			Optional<ItemPrototype> item = FactorioManager.lookupItemByName(itemName);
			if (item.isPresent() && item.get().getType().equals("module")) {
				modules.add(itemName, itemStack.itemsInInventory.size());
			}
		}

		return modules;
	}

	public static void halveAlpha(BufferedImage image) {
		int w = image.getWidth();
		int h = image.getHeight();
		int[] pixels = new int[w * h];
		image.getRGB(0, 0, w, h, pixels, 0, w);
		for (int i = 0; i < pixels.length; i++) {
			int argb = pixels[i];
			int a = ((argb >> 24) & 0xFF);

			pixels[i] = ((a / 2) << 24) | (argb & 0xFFFFFF);
		}
		image.setRGB(0, 0, w, h, pixels, 0, w);
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

	public static void shiftSprites(List<Sprite> sprites, Point2D.Double shift) {
		for (Sprite sprite : sprites) {
			sprite.bounds.x += shift.x;
			sprite.bounds.y += shift.y;
		}
	}

	public static EntityRenderer spriteRenderer(List<Sprite> sprites, BSEntity entity, MapRect3D bounds) {
		return spriteRenderer(sprites, entity.position.createPoint(), bounds.rotate(entity.direction));
	}

	public static EntityRenderer spriteRenderer(List<Sprite> sprites, Point2D.Double pos, MapRect3D bounds) {
		RenderUtils.shiftSprites(sprites, pos);

		Map<Boolean, List<Sprite>> groupedSprites = sprites.stream()
				.collect(Collectors.partitioningBy(sprite -> sprite.layer == Layer.SHADOW_BUFFER));

		bounds = bounds.shift(pos.x, pos.y);

		return new EntityRenderer(layer, bounds, false) {
			@Override
			public void render(Graphics2D g) {
				for (Sprite sprite : groupedSprites.get(false)) {
					drawSprite(sprite, g);
				}
			}

			@Override
			public void renderShadows(Graphics2D g) {
				for (Sprite sprite : groupedSprites.get(true)) {
					drawSprite(sprite, g);
				}
			}
		};
	}

	public static EntityRenderer spriteRenderer(Sprite sprite, BSEntity entity, MapRect3D bounds) {
		return spriteRenderer(ImmutableList.of(sprite), entity, bounds);
	}

	public static EntityRenderer spriteRenderer(Sprite sprite, Point2D.Double pos, MapRect3D bounds) {
		return spriteRenderer(ImmutableList.of(sprite), pos, bounds);
	}

	public static Renderer spriteRenderer(Sprite sprite, MapRect3D bounds) {
		return new Renderer(sprite.layer, bounds, false) {
			@SuppressWarnings("unused")
			private void debugShowBounds(Rectangle2D.Double groundBounds, Graphics2D g) {
				long x = Math.round(groundBounds.getCenterX() * 2);
				long y = Math.round(groundBounds.getCenterY() * 2);
				long w = Math.round(groundBounds.width * 2);
				long h = Math.round(groundBounds.height * 2);

				// System.out.println("x=" + x + " y=" + y + " w=" + w + "
				// h=" + h);

				g.setColor(new Color(255, 255, 255, 64));
				g.draw(groundBounds);

				if (((w / 2) % 2) == (x % 2)) {
					g.setColor(new Color(255, 0, 0, 64));
					g.fill(groundBounds);
				}
				if (((h / 2) % 2) == (y % 2)) {
					g.setColor(new Color(0, 255, 0, 64));
					g.fill(groundBounds);
				}
			}

			@Override
			public void render(Graphics2D g) {
				drawSprite(sprite, g);
				// debugShowBounds(groundBounds, g);
			}
		};
	}

	public static Color withAlpha(Color color, int alpha) {
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}

	private RenderUtils() {
	}

}
