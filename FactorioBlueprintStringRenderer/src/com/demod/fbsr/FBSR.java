package com.demod.fbsr;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.json.JSONException;
import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.ModInfo;
import com.demod.factorio.Utils;
import com.demod.factorio.prototype.DataPrototype;
import com.demod.fbsr.render.Renderer;
import com.demod.fbsr.render.TypeRendererFactory;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;

public class FBSR {

	private static class RenderingTuple {
		BlueprintEntity entity;
		DataPrototype prototype;
		TypeRendererFactory factory;
	}

	private static final Color GROUND_COLOR = new Color(40, 40, 40);
	private static final Color GRID_COLOR = new Color(60, 60, 60);

	private static final BasicStroke GRID_STROKE = new BasicStroke((float) (3 / TypeRendererFactory.tileSize));

	private static final double FOOTER_HEIGHT = 0.5;

	private static Map<String, BufferedImage> modImageCache = new HashMap<>();

	private static volatile String version = null;

	private static void alignRenderingTuplesToGrid(List<RenderingTuple> renderingTuples) {
		Multiset<Boolean> xAligned = LinkedHashMultiset.create();
		Multiset<Boolean> yAligned = LinkedHashMultiset.create();

		for (RenderingTuple tuple : renderingTuples) {
			if (tuple.entity.getName().equals("straight-rail") || tuple.entity.getName().equals("curved-rail")) {
				continue; // XXX
			}

			Rectangle2D.Double bounds = Utils.parseRectangle(tuple.prototype.lua().get("selection_box"));
			Point2D.Double position = tuple.entity.getPosition();
			bounds.x += position.x;
			bounds.y += position.y;
			// System.out.println(bounds);

			// Everything is doubled and rounded to the closest original 0.5
			// increment
			long x = Math.round(bounds.getCenterX() * 2);
			long y = Math.round(bounds.getCenterY() * 2);
			long w = Math.round(bounds.width * 2);
			long h = Math.round(bounds.height * 2);

			// System.out.println("x=" + x + " y=" + y + " w=" + w + " h=" + h +
			// " alignX=" + (((w / 2) % 2) == (x % 2))
			// + " alignY=" + (((h / 2) % 2 == 0) == (y % 2 == 0)));

			// If size/2 is odd, pos should be odd, and vice versa
			xAligned.add(((w / 2) % 2 == 0) == (x % 2 == 0));
			yAligned.add(((h / 2) % 2 == 0) == (y % 2 == 0));
		}

		System.out.println("X ALIGNED: " + xAligned.count(true) + " yes, " + xAligned.count(false) + " no");
		System.out.println("Y ALIGNED: " + yAligned.count(true) + " yes, " + yAligned.count(false) + " no");

		boolean shiftX = xAligned.count(true) < xAligned.count(false);
		boolean shiftY = yAligned.count(true) < yAligned.count(false);
		if (shiftX || shiftY) {
			System.out.println("SHIFTING!");
			for (RenderingTuple tuple : renderingTuples) {
				Point2D.Double position = tuple.entity.getPosition();
				if (shiftX) {
					position.x += 0.5;
				}
				if (shiftY) {
					position.y += 0.5;
				}
			}
		}
	}

	private static BufferedImage applyRendering(BlueprintReporting reporting, int tileSize, List<Renderer> renderers)
			throws JSONException, FileNotFoundException, IOException {
		Area area = new Area();
		renderers.forEach(r -> area.add(new Area(r.getBounds())));
		Rectangle2D bounds = area.getBounds2D();
		bounds.setFrameFromDiagonal(Math.floor(bounds.getMinX()) - 1, Math.floor(bounds.getMinY()) - 1,
				Math.ceil(bounds.getMaxX()) + 1, Math.ceil(bounds.getMaxY()) + 1 + FOOTER_HEIGHT);
		BufferedImage image = new BufferedImage((int) (bounds.getWidth() * tileSize),
				(int) (bounds.getHeight() * tileSize), BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

		g.scale(image.getWidth() / bounds.getWidth(), image.getHeight() / bounds.getHeight());
		g.translate(-bounds.getX(), -bounds.getY());
		AffineTransform worldXform = g.getTransform();

		// Background
		g.setColor(GROUND_COLOR);
		g.fill(bounds);

		// Grid Lines
		g.setStroke(GRID_STROKE);
		g.setColor(GRID_COLOR);
		for (double x = Math.round(bounds.getMinX()); x <= bounds.getMaxX(); x++) {
			g.draw(new Line2D.Double(x, bounds.getMinY(), x, bounds.getMaxY()));
		}
		for (double y = Math.round(bounds.getMinY()); y <= bounds.getMaxY(); y++) {
			g.draw(new Line2D.Double(bounds.getMinX(), y, bounds.getMaxX(), y));
		}

		// Footer
		g.setColor(GROUND_COLOR);
		g.fill(new Rectangle2D.Double(bounds.getMinX(),
				bounds.getMaxY() - FOOTER_HEIGHT + GRID_STROKE.getLineWidth() / 2f, bounds.getWidth(), FOOTER_HEIGHT));
		g.setColor(GRID_COLOR);
		g.setFont(new Font("Monospaced", Font.BOLD, 1).deriveFont(0.4f));
		String footerMessage;
		if (bounds.getWidth() > 5.5) {
			footerMessage = "Made by BlueprintBot - Factorio " + getVersion();
		} else {
			footerMessage = "BlueprintBot";
		}
		g.drawString(footerMessage, (float) (bounds.getMinX() + 0.11), (float) (bounds.getMaxY() - 0.11));

		renderers.stream().sorted((r1, r2) -> {
			int ret;

			ret = r1.getLayer().compareTo(r2.getLayer());
			if (ret != 0) {
				return ret;
			}

			Rectangle2D.Double b1 = r1.getBounds();
			Rectangle2D.Double b2 = r2.getBounds();

			ret = Double.compare(b1.getMinY(), b2.getMinY());
			if (ret != 0) {
				return ret;
			}

			ret = Double.compare(b1.getMinX(), b2.getMinX());
			if (ret != 0) {
				return ret;
			}

			ret = r1.getLayer().compareTo(r2.getLayer());
			return ret;
		}).forEach(r -> {
			try {
				r.render(g);
			} catch (Exception e) {
				reporting.addException(e);
			}
		});
		g.setTransform(worldXform);

		// g.setColor(Color.cyan);
		// Rectangle2D.Double dot = new Rectangle2D.Double(0, 0, 0.1, 0.1);
		// renderers.forEach(r -> {
		// dot.x = r.getBounds().getCenterX() - dot.width / 2.0;
		// dot.y = r.getBounds().getCenterY() - dot.height / 2.0;
		// g.draw(dot);
		// });

		// Grid Numbers
		g.setColor(GRID_COLOR);
		g.setFont(new Font("Monospaced", Font.BOLD, 1).deriveFont(0.6f));
		for (double x = Math.round(bounds.getMinX()) + 1, i = 1; x <= bounds.getMaxX() - 2; x++, i++) {
			g.drawString(String.format("%02d", (int) Math.round(i) % 100), (float) x + 0.2f,
					(float) (bounds.getMaxY() - 1 + 0.65f - FOOTER_HEIGHT));
			g.drawString(String.format("%02d", (int) Math.round(i) % 100), (float) x + 0.2f,
					(float) (bounds.getMinY() + 0.65f));
		}
		for (double y = Math.round(bounds.getMinY()) + 1, i = 1; y <= bounds.getMaxY() - 2; y++, i++) {
			g.drawString(String.format("%02d", (int) Math.round(i) % 100), (float) (bounds.getMaxX() - 1 + 0.2f),
					(float) y + 0.65f);
			g.drawString(String.format("%02d", (int) Math.round(i) % 100), (float) (bounds.getMinX() + 0.2f),
					(float) y + 0.65f);
		}

		g.dispose();
		return image;
	}

	public static synchronized BufferedImage getModImage(LuaValue value) {
		String path = value.toString();
		if (path.isEmpty()) {
			throw new IllegalArgumentException("Path is Empty!");
		}
		return modImageCache.computeIfAbsent(path, p -> {
			String firstSegment = path.split("\\/")[0];
			String mod = firstSegment.substring(2, firstSegment.length() - 2);
			File modFolder = new File(FactorioData.factorio, "data/" + mod);
			File file = new File(modFolder, path.replace(firstSegment, "").substring(1));
			try {
				return ImageIO.read(file);
			} catch (IOException e) {
				System.err.println(file.getAbsolutePath());
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		});
	}

	private static String getVersion() throws JSONException, FileNotFoundException, IOException {
		if (version == null) {
			ModInfo baseInfo = new ModInfo(Utils
					.readJsonFromStream(new FileInputStream(new File(FactorioData.factorio, "data/base/info.json"))));
			version = baseInfo.getVersion();
		}
		return version;
	}

	public static BufferedImage renderBlueprint(Blueprint blueprint, BlueprintReporting reporting)
			throws JSONException, IOException {
		DataTable table = FactorioData.getTable();

		List<RenderingTuple> renderingTuples = new ArrayList<RenderingTuple>();
		for (BlueprintEntity entity : blueprint.getEntities()) {
			RenderingTuple tuple = new RenderingTuple();
			tuple.entity = entity;
			tuple.prototype = table.getEntities().get(entity.getName());
			if (tuple.prototype == null) {
				reporting.addWarning("Cant find prototype for " + entity.getName());
				continue;
			}
			tuple.factory = TypeRendererFactory.forType(tuple.prototype.getType());
			// System.out.println("\t" + entity.getName() + " -> " +
			// tuple.factory.getClass().getSimpleName());
			renderingTuples.add(tuple);
		}
		alignRenderingTuplesToGrid(renderingTuples);

		WorldMap worldMap = new WorldMap();
		renderingTuples.forEach(t -> {
			try {
				t.factory.populateWorldMap(worldMap, table, t.entity, t.prototype);
			} catch (Exception e) {
				reporting.addException(e);
			}
		});

		List<Renderer> renderers = new ArrayList<>();

		renderingTuples.forEach(t -> {
			try {
				t.factory.createRenderers(renderers::add, worldMap, table, t.entity, t.prototype);
			} catch (Exception e) {
				reporting.addException(e);
			}
		});

		renderingTuples.forEach(t -> {
			try {
				t.factory.createWireConnections(renderers::add, worldMap, table, t.entity, t.prototype);
			} catch (Exception e) {
				reporting.addException(e);
			}
		});

		return applyRendering(reporting, (int) Math.round(TypeRendererFactory.tileSize), renderers);
	}
}
