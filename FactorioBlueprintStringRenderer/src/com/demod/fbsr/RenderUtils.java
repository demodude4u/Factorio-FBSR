package com.demod.fbsr;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.luaj.vm2.LuaValue;

import com.demod.factorio.FactorioData;
import com.demod.factorio.Utils;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.factorio.prototype.TilePrototype;
import com.demod.fbsr.Renderer.Layer;
import com.google.common.collect.ImmutableList;

public final class RenderUtils {
	public static final BufferedImage EMPTY_IMAGE = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
	static {
		EMPTY_IMAGE.setRGB(0, 0, 0x00000000);
	}

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

	public static void drawSprite(Sprite sprite, Graphics2D g) {
		drawImageInBounds(sprite.image, sprite.source, sprite.bounds, g);
	}

	public static String fmtDouble(double value) {
		if (value == (long) value) {
			return String.format("%d", (long) value);
		} else {
			return Double.toString(value);// String.format("%f", value);
		}
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

	public static Sprite getSpriteFromAnimation(LuaValue lua) {
		LuaValue hrVersion = lua.get("hr_version");
		if (!hrVersion.isnil()) {
			Utils.forEach(hrVersion, (k, v) -> {
				lua.set(k, v);
			});
			lua.set("hr_version", LuaValue.NIL);
		}

		Sprite ret = new Sprite();
		String imagePath;
		if (!lua.get("filenames").isnil()) {
			// XXX this is a hack, assuming artillery turret code
			int direction = lua.get("artillery_direction").toint();
			imagePath = lua.get("filenames").get(direction * 2 + 1).tojstring();
		} else {
			imagePath = lua.get("filename").tojstring();
		}
		boolean drawAsShadow = lua.get("draw_as_shadow").optboolean(false);
		ret.shadow = drawAsShadow;
		// drawAsShadow = false;// FIXME shadows need a special mask layer
		if (drawAsShadow) {
			ret.image = FactorioData.getModImage(imagePath, new Color(255, 255, 255, 128));
		} else {
			ret.image = FactorioData.getModImage(imagePath);
		}
		String blendMode = lua.get("blend_mode").optjstring("normal");
		if (!blendMode.equals("normal")) { // FIXME blending will take effort
			ret.image = EMPTY_IMAGE;
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
		List<Sprite> sprites = new ArrayList<>();
		LuaValue layersLua = lua.get("layers");
		if (layersLua.isnil()) {
			layersLua = lua.get("sheets");
		}
		if (!layersLua.isnil()) {
			Utils.forEach(layersLua.checktable(), (i, l) -> {
				Sprite sprite = getSpriteFromAnimation(l);
				sprite.order = i.toint();
				sprites.add(sprite);
			});
		} else {
			sprites.add(getSpriteFromAnimation(lua));
		}

		sprites.sort((s1, s2) -> {
			if (s1.shadow != s2.shadow) {
				return Boolean.compare(s2.shadow, s1.shadow);
			}
			return Integer.compare(s2.order, s1.order);
		});

		return sprites;
	}

	public static List<Sprite> getSpritesFromAnimation(LuaValue lua, Direction direction) {
		LuaValue dirLua = lua.get(direction.name().toLowerCase());
		if (!dirLua.isnil()) {
			return getSpritesFromAnimation(dirLua);
		} else {
			return getSpritesFromAnimation(lua);
		}
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

	public static Renderer spriteRenderer(Layer layer, List<Sprite> sprites, BlueprintEntity entity,
			EntityPrototype prototype) {
		Point2D.Double pos = entity.getPosition();
		for (Sprite sprite : sprites) {
			sprite.bounds.x += pos.x;
			sprite.bounds.y += pos.y;
		}
		// Rectangle2D.Double groundBounds =
		// Utils.parseRectangle(prototype.lua().get("collision_box"));
		Rectangle2D.Double groundBounds = Utils.parseRectangle(prototype.lua().get("selection_box"));
		groundBounds.x += pos.x;
		groundBounds.y += pos.y;
		return new Renderer(layer, groundBounds) {
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
				for (Sprite sprite : sprites) {
					drawSprite(sprite, g);
					// debugShowBounds(groundBounds, g);
				}
			}
		};
	}

	public static Renderer spriteRenderer(Layer layer, Sprite sprite, BlueprintEntity entity,
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

	public static Renderer spriteRenderer(List<Sprite> sprites, BlueprintEntity entity, EntityPrototype prototype) {
		return spriteRenderer(Layer.ENTITY, sprites, entity, prototype);
	}

	public static Renderer spriteRenderer(Sprite sprite, BlueprintEntity entity, EntityPrototype prototype) {
		return spriteRenderer(Layer.ENTITY, sprite, entity, prototype);
	}

	public static Color withAlpha(Color color, int alpha) {
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}

	private RenderUtils() {
	}
}
