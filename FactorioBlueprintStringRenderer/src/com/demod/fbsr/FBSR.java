package com.demod.fbsr;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
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

public class FBSR {

	private static final Color GROUND_COLOR = new Color(40, 40, 40);

	private static final Color GRID_COLOR = new Color(60, 60, 60);
	private static final BasicStroke GRID_STROKE = new BasicStroke((float) (3 / TypeRendererFactory.tileSize));

	private static final double FOOTER_HEIGHT = 0.5;

	private static Map<String, BufferedImage> modImageCache = new HashMap<>();

	private static volatile String version = null;

	private static BufferedImage applyRendering(int tileSize, List<Renderer> renderers)
			throws JSONException, FileNotFoundException, IOException {
		Area area = new Area();
		renderers.forEach(r -> area.add(new Area(r.getBounds())));
		Rectangle2D bounds = area.getBounds2D();
		bounds.setFrameFromDiagonal(Math.floor(bounds.getMinX()) - 0.5, Math.floor(bounds.getMinY()) - 0.5,
				Math.ceil(bounds.getMaxX()) + 0.5, Math.ceil(bounds.getMaxY()) + 0.5 + FOOTER_HEIGHT);
		BufferedImage image = new BufferedImage((int) (bounds.getWidth() * tileSize),
				(int) (bounds.getHeight() * tileSize), BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		g.scale(image.getWidth() / bounds.getWidth(), image.getHeight() / bounds.getHeight());
		g.translate(-bounds.getX(), -bounds.getY());

		// Background
		g.setColor(GROUND_COLOR);
		g.fill(bounds);

		// Grid Lines
		g.setStroke(GRID_STROKE);
		g.setColor(GRID_COLOR);
		for (double x = Math.round(bounds.getMinX()) - 0.5; x <= bounds.getMaxX(); x++) {
			g.draw(new Line2D.Double(x, bounds.getMinY(), x, bounds.getMaxY()));
		}
		for (double y = Math.round(bounds.getMinY()) - 0.5; y <= bounds.getMaxY(); y++) {
			g.draw(new Line2D.Double(bounds.getMinX(), y, bounds.getMaxX(), y));
		}

		// Grid Numbers
		g.setColor(GRID_COLOR);
		g.setFont(new Font("Monospaced", Font.BOLD, 1).deriveFont(0.6f));
		for (double x = Math.round(bounds.getMinX()) + 0.5, i = 1; x <= bounds.getMaxX() - 2; x++, i++) {
			g.drawString(String.format("%02d", (int) Math.round(i)), (float) x + 0.2f,
					(float) (bounds.getMaxY() - 1 + 0.65f - FOOTER_HEIGHT));
			g.drawString(String.format("%02d", (int) Math.round(i)), (float) x + 0.2f,
					(float) (bounds.getMinY() + 0.65f));
		}
		for (double y = Math.round(bounds.getMinY()) + 0.5, i = 1; y <= bounds.getMaxY() - 2; y++, i++) {
			g.drawString(String.format("%02d", (int) Math.round(i) % 100), (float) (bounds.getMaxX() - 1 + 0.2f),
					(float) y + 0.65f);
			g.drawString(String.format("%02d", (int) Math.round(i) % 100), (float) (bounds.getMinX() + 0.2f),
					(float) y + 0.65f);
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
		}).forEach(r -> r.render(g));

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

	public static BufferedImage renderBlueprint(Blueprint blueprint) throws JSONException, IOException {
		DataTable table = FactorioData.getTable();

		class RenderingTuple {
			BlueprintEntity entity;
			DataPrototype prototype;
			TypeRendererFactory factory;
		}
		List<RenderingTuple> renderingTuples = new ArrayList<RenderingTuple>();
		for (BlueprintEntity entity : blueprint.getEntities()) {
			RenderingTuple tuple = new RenderingTuple();
			tuple.entity = entity;
			tuple.prototype = table.getEntities().get(entity.getName());
			if (tuple.prototype == null) {
				System.err.println("Cant find prototype for " + entity.getName());
				continue;
			}
			tuple.factory = TypeRendererFactory.forType(tuple.prototype.getType());
			renderingTuples.add(tuple);
		}

		WorldMap worldState = new WorldMap();
		renderingTuples.forEach(t -> t.factory.populateWorldMap(worldState, table, t.entity, t.prototype));

		List<Renderer> renderers = new ArrayList<>();
		renderingTuples
				.forEach(t -> t.factory.createRenderers(renderers::add, worldState, table, t.entity, t.prototype));

		return applyRendering((int) Math.round(TypeRendererFactory.tileSize), renderers);
	}
}
