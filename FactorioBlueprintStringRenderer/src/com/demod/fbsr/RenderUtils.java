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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.Utils;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.factorio.prototype.ItemPrototype;
import com.demod.factorio.prototype.TilePrototype;
import com.demod.fbsr.Renderer.Layer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;

public final class RenderUtils {
	public static final BufferedImage EMPTY_IMAGE = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
	static {
		EMPTY_IMAGE.setRGB(0, 0, 0x00000000);
	}

	private static final DecimalFormat DECIMAL_FORMAT_2_PLACES = new DecimalFormat("#,##0.##");

	public static Renderer createWireRenderer(Point2D.Double p1, Point2D.Double p2, Color color) {
		Rectangle2D.Double bounds = new Rectangle2D.Double();
		bounds.setFrameFromDiagonal(p1, p2);

		return new Renderer(Layer.WIRE, bounds) {
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
		return new Renderer(layer, position) {
			@Override
			public void render(Graphics2D g) {
				AffineTransform pat = g.getTransform();

				g.setFont(new Font("Monospaced", Font.BOLD, 1).deriveFont(0.4f));
				float textX = (float) bounds.x;
				float textY = (float) bounds.y;

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
		return new Renderer(layer, position) {
			@Override
			public void render(Graphics2D g) {
				g.setFont(new Font("Monospaced", Font.BOLD, 1).deriveFont(0.4f));
				float textX = (float) bounds.x;
				float textY = (float) bounds.y;
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

	public static Optional<Multiset<String>> getModules(BlueprintEntity entity, DataTable table) {
		// TODO new format
		if (entity.isJsonNewFormat() || !entity.json().has("items")) {
			return Optional.empty();
		}

		Multiset<String> modules = LinkedHashMultiset.create();

		Object itemsJson = entity.json().get("items");
		if (itemsJson instanceof JSONObject) {
			Utils.forEach(entity.json().getJSONObject("items"), (String itemName, Integer count) -> {
				modules.add(itemName, count);
			});
		} else if (itemsJson instanceof JSONArray) {
			Utils.<JSONObject>forEach(entity.json().getJSONArray("items"), j -> {
				modules.add(j.getString("item"), j.getInt("count"));
			});
		}

		modules.entrySet().removeIf(e -> {
			Optional<ItemPrototype> item = table.getItem(e.getElement());
			return !item.isPresent() || !item.get().getType().equals("module");
		});

		return Optional.of(modules);
	}

	public static Sprite getSpriteFromAnimation(LuaValue lua) {
		return getSpriteFromAnimation(lua, 0);
	}

	/***
	 * @param fileNameSelector The 1-based index of the desired image path in the
	 *                         "filenames" array. Used only if "filenames" property
	 *                         exists.
	 */
	public static Sprite getSpriteFromAnimation(LuaValue lua, int fileNameSelector) {
		LuaValue sheetLua = lua.get("sheet");
		if (!sheetLua.isnil()) {
			lua = sheetLua;
		}

		LuaValue hrVersion = lua.get("hr_version");
		if (!hrVersion.isnil()) {
			lua = hrVersion;
		}

		Sprite ret = new Sprite();
		String imagePath;
		if (!lua.get("filenames").isnil()) {
			// if (fileNameSelector == 0)
			// System.err.println("Using 'filenames' but file name selector is not set!");
			// if the above happens, it will automatically throw with the below line
			imagePath = lua.get("filenames").get(fileNameSelector).tojstring();
		} else {
			imagePath = lua.get("filename").tojstring();
		}
		ret.image = FactorioData.getModImage(imagePath);

		boolean drawAsShadow = lua.get("draw_as_shadow").optboolean(false);
		ret.shadow = drawAsShadow;

		String blendMode = lua.get("blend_mode").optjstring("normal");
		if (!blendMode.equals("normal")) { // FIXME blending will take effort
			ret.image = EMPTY_IMAGE;
		}
		LuaValue tint = lua.get("tint");
		if (!tint.isnil()) {
			ret.image = Utils.tintImage(ret.image, Utils.parseColor(tint));
		}
		double scale = lua.get("scale").optdouble(1.0);
		int srcX = lua.get("x").optint(0);
		int srcY = lua.get("y").optint(0);
		int srcWidth = lua.get("width").checkint();
		double width = scale * srcWidth / FBSR.tileSize;
		int srcHeight = lua.get("height").checkint();
		double height = scale * srcHeight / FBSR.tileSize;
		Point2D.Double shift = Utils.parsePoint2D(lua.get("shift"));
		ret.source = new Rectangle(srcX, srcY, srcWidth, srcHeight);
		ret.bounds = new Rectangle2D.Double(shift.x - width / 2.0, shift.y - height / 2.0, width, height);
		return ret;
	}

	public static List<Sprite> getSpritesFromAnimation(LuaValue lua) {
		return getSpritesFromAnimation(lua, 0);
	}

	public static List<Sprite> getSpritesFromAnimation(LuaValue lua, Direction direction) {
		LuaValue dirLua = lua.get(direction.name().toLowerCase());
		if (!dirLua.isnil()) {
			return getSpritesFromAnimation(dirLua, 0);
		} else {
			return getSpritesFromAnimation(lua, 0);
		}
	}

	/***
	 * @param fileNameSelector The 1-based index of the desired image path in the
	 *                         "filenames" array. Used only if "filenames" property
	 *                         exists.
	 */
	public static List<Sprite> getSpritesFromAnimation(LuaValue lua, int fileNameSelector) {
		List<Sprite> sprites = new ArrayList<>();
		LuaValue layersLua = lua.get("layers");
		if (layersLua.isnil()) {
			layersLua = lua.get("sheets");
		}
		if (!layersLua.isnil()) {
			Utils.forEach(layersLua.checktable(), (i, l) -> {
				Sprite sprite = getSpriteFromAnimation(l, fileNameSelector);
				sprite.order = i.toint();
				sprites.add(sprite);
			});
		} else {
			sprites.add(getSpriteFromAnimation(lua, fileNameSelector));
		}

		sprites.sort((s1, s2) -> {
			if (s1.shadow != s2.shadow) {
				return Boolean.compare(s2.shadow, s1.shadow);
			}
			return Integer.compare(s2.order, s1.order);
		});

		return sprites;
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

	public static EntityRenderer spriteRenderer(Layer layer, List<Sprite> sprites, BlueprintEntity entity,
			EntityPrototype prototype) {
		Point2D.Double pos = entity.getPosition();
		RenderUtils.shiftSprites(sprites, pos);

		Map<Boolean, List<Sprite>> groupedSprites = sprites.stream()
				.collect(Collectors.partitioningBy(sprite -> sprite.shadow));

		// Rectangle2D.Double groundBounds =
		// Utils.parseRectangle(prototype.lua().get("collision_box"));
		Rectangle2D.Double groundBounds = Utils.parseRectangle(prototype.lua().get("selection_box"));
		groundBounds.x += pos.x;
		groundBounds.y += pos.y;
		return new EntityRenderer(layer, groundBounds) {
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
				for (Sprite sprite : groupedSprites.get(false)) {
					drawSprite(sprite, g);
					// debugShowBounds(groundBounds, g);
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

	public static EntityRenderer spriteRenderer(Layer layer, Sprite sprite, BlueprintEntity entity,
			EntityPrototype prototype) {
		return spriteRenderer(layer, ImmutableList.of(sprite), entity, prototype);
	}

	public static Renderer spriteRenderer(Layer layer, Sprite sprite, BlueprintTile tile, TilePrototype prototype) {
		Point2D.Double pos = tile.getPosition();
		sprite.bounds.x += pos.x;
		sprite.bounds.y += pos.y;

		return new Renderer(layer, new Rectangle2D.Double(pos.x - 0.5, pos.y - 0.5, 1.0, 1.0)) {
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

	public static EntityRenderer spriteRenderer(List<Sprite> sprites, BlueprintEntity entity,
			EntityPrototype prototype) {
		return spriteRenderer(Layer.ENTITY, sprites, entity, prototype);
	}

	public static EntityRenderer spriteRenderer(Sprite sprite, BlueprintEntity entity, EntityPrototype prototype) {
		return spriteRenderer(Layer.ENTITY, sprite, entity, prototype);
	}

	public static Color withAlpha(Color color, int alpha) {
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}

	private RenderUtils() {
	}

}
