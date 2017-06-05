package com.demod.fbsr;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.json.JSONException;

import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.ModInfo;
import com.demod.factorio.Utils;
import com.demod.factorio.prototype.DataPrototype;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.Renderer.Layer;
import com.demod.fbsr.render.TypeRendererFactory;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Table;

import javafx.util.Pair;

public class FBSR {

	private static class RenderingTuple {
		BlueprintEntity entity;
		EntityPrototype prototype;
		TypeRendererFactory factory;
	}

	private static final Color GROUND_COLOR = new Color(40, 40, 40);
	private static final Color GRID_COLOR = new Color(60, 60, 60);

	private static final BasicStroke GRID_STROKE = new BasicStroke((float) (3 / TypeRendererFactory.tileSize));

	private static final double FOOTER_HEIGHT = 0.5;

	private static volatile String version = null;

	private static final Map<String, Color> itemColorCache = new HashMap<>();

	private static void alignRenderingTuplesToGrid(List<RenderingTuple> renderingTuples) {
		Multiset<Boolean> xAligned = LinkedHashMultiset.create();
		Multiset<Boolean> yAligned = LinkedHashMultiset.create();

		for (RenderingTuple tuple : renderingTuples) {
			if (tuple.entity.getName().equals("straight-rail") || tuple.entity.getName().equals("curved-rail")) {
				continue; // XXX
			}

			Rectangle2D.Double selectionBox = tuple.prototype.getSelectionBox();
			Point2D.Double position = tuple.entity.getPosition();
			Rectangle2D.Double bounds = new Rectangle2D.Double(selectionBox.x + position.x, selectionBox.y + position.y,
					selectionBox.width, selectionBox.height);
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
		Rectangle2D.Double bounds = computeBounds(renderers);
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

	private static Rectangle2D.Double computeBounds(List<Renderer> renderers) {
		if (renderers.isEmpty()) {
			return new Rectangle2D.Double();
		}
		boolean first = true;
		double minX = 0, minY = 0, maxX = 0, maxY = 0;
		for (Renderer renderer : renderers) {
			Rectangle2D.Double bounds = renderer.bounds;
			if (first) {
				first = false;
				minX = bounds.getMinX();
				minY = bounds.getMinY();
				maxX = bounds.getMaxX();
				maxY = bounds.getMaxY();
			} else {
				minX = Math.min(minX, bounds.getMinX());
				minY = Math.min(minY, bounds.getMinY());
				maxX = Math.max(maxX, bounds.getMaxX());
				maxY = Math.max(maxY, bounds.getMaxY());
			}
		}
		return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
	}

	private static Color getItemLogisticColor(DataTable table, String itemName) {
		return itemColorCache.computeIfAbsent(itemName, k -> {
			Optional<DataPrototype> optProto = table.getItem(k);
			if (!optProto.isPresent()) {
				System.err.println("ITEM MISSING FOR LOGISTICS: " + k);
				return Color.MAGENTA;
			}
			DataPrototype prototype = optProto.get();
			BufferedImage image = FactorioData.getModImage(prototype.lua().get("icon"));
			Color color = Utils.getAverageColor(image);
			// return new Color(color.getRGB() | 0xA0A0A0);
			// return color.brighter().brighter();
			float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
			return Color.getHSBColor(hsb[0], Math.max(0.25f, hsb[1]), Math.max(0.5f, hsb[2]));
			// return Color.getHSBColor(hsb[0], Math.max(1f, hsb[1]),
			// Math.max(0.75f, hsb[2]));
		});
	}

	public static String getVersion() throws JSONException, FileNotFoundException, IOException {
		if (version == null) {
			ModInfo baseInfo = new ModInfo(Utils
					.readJsonFromStream(new FileInputStream(new File(FactorioData.factorio, "data/base/info.json"))));
			version = baseInfo.getVersion();
		}
		return version;
	}

	private static void populateReverseLogistics(WorldMap map) {
		Table<Integer, Integer, LogisticGridCell> logisticGrid = map.getLogisticGrid();
		logisticGrid.cellSet().forEach(c -> {
			Point2D.Double pos = new Point2D.Double(c.getRowKey() / 2.0 + 0.25, c.getColumnKey() / 2.0 + 0.25);
			LogisticGridCell cell = c.getValue();
			cell.getMove().ifPresent(d -> {
				map.getLogisticGridCell(d.offset(pos, 0.5)).filter(mc -> mc.acceptMoveFrom(d))
						.ifPresent(mc -> mc.addMovedFrom(d.back()));
			});
			cell.getWarp().ifPresent(p -> {
				map.getLogisticGridCell(p).ifPresent(mc -> mc.addWarpedFrom(pos));
			});
		});
	}

	private static void populateTransitLogistics(WorldMap map) {
		Table<Integer, Integer, LogisticGridCell> logisticGrid = map.getLogisticGrid();
		ArrayDeque<Pair<Point2D.Double, LogisticGridCell>> work = new ArrayDeque<>();

		logisticGrid.cellSet().stream().filter(c -> c.getValue().isTransitStart()).forEach(c -> {
			Set<String> outputs = c.getValue().getOutputs().get();
			for (String item : outputs) {
				work.add(new Pair<>(map.getLogisticCellPosition(c), c.getValue()));
				while (!work.isEmpty()) {
					Pair<Point2D.Double, LogisticGridCell> pair = work.pop();
					Point2D.Double cellPos = pair.getKey();
					LogisticGridCell cell = pair.getValue();
					if (cell.addTransit(item)) {
						cell.getMove().ifPresent(d -> {
							Point2D.Double nextCellPos = d.offset(cellPos, 0.5);
							map.getLogisticGridCell(nextCellPos)
									.filter(nc -> !nc.isBlockTransit() && nc.acceptMoveFrom(d))
									.ifPresent(next -> work.add(new Pair<>(nextCellPos, next)));
						});
						cell.getWarp().ifPresent(p -> {
							map.getLogisticGridCell(p).filter(nc -> !nc.isBlockTransit())
									.ifPresent(next -> work.add(new Pair<>(p, next)));
						});
					}
				}
			}
		});

		logisticGrid.cellSet().stream().filter(c -> c.getValue().isTransitEnd()).forEach(c -> {
			Set<String> inputs = c.getValue().getInputs().get();
			for (String item : inputs) {
				work.add(new Pair<>(map.getLogisticCellPosition(c), c.getValue()));
				while (!work.isEmpty()) {
					Pair<Point2D.Double, LogisticGridCell> pair = work.pop();
					Point2D.Double cellPos = pair.getKey();
					LogisticGridCell cell = pair.getValue();
					if (cell.addTransit(item)) {
						cell.getMovedFrom().ifPresent(l -> {
							for (Direction d : l) {
								Point2D.Double nextCellPos = d.offset(cellPos, 0.5);
								map.getLogisticGridCell(nextCellPos).filter(nc -> !nc.isBlockTransit())
										.ifPresent(next -> work.add(new Pair<>(nextCellPos, next)));
							}
						});
						cell.getWarpedFrom().ifPresent(l -> {
							for (Point2D.Double p : l) {
								map.getLogisticGridCell(p).filter(nc -> !nc.isBlockTransit())
										.ifPresent(next -> work.add(new Pair<>(p, next)));
							}
						});
					}
				}
			}
		});

	}

	public static BufferedImage renderBlueprint(Blueprint blueprint, BlueprintReporting reporting)
			throws JSONException, IOException {
		DataTable table = FactorioData.getTable();
		WorldMap map = new WorldMap();
		reporting.getDebug().ifPresent(map::setDebug);

		List<RenderingTuple> renderingTuples = new ArrayList<RenderingTuple>();
		for (BlueprintEntity entity : blueprint.getEntities()) {
			RenderingTuple tuple = new RenderingTuple();
			tuple.entity = entity;
			Optional<EntityPrototype> prototype = table.getEntity(entity.getName());
			if (!prototype.isPresent()) {
				reporting.addWarning("Cant find prototype for " + entity.getName());
				continue;
			}
			tuple.prototype = prototype.get();
			tuple.factory = TypeRendererFactory.forType(tuple.prototype.getType());
			// System.out.println("\t" + entity.getName() + " -> " +
			// tuple.factory.getClass().getSimpleName());
			renderingTuples.add(tuple);
		}
		alignRenderingTuplesToGrid(renderingTuples);

		renderingTuples.forEach(t -> {
			try {
				t.factory.populateWorldMap(map, table, t.entity, t.prototype);
			} catch (Exception e) {
				reporting.addException(e);
			}
		});

		renderingTuples.forEach(t -> {
			try {
				t.factory.populateLogistics(map, table, t.entity, t.prototype);
			} catch (Exception e) {
				reporting.addException(e);
			}
		});

		populateReverseLogistics(map);

		populateTransitLogistics(map);

		List<Renderer> renderers = new ArrayList<>();

		renderingTuples.forEach(t -> {
			try {
				t.factory.createRenderers(renderers::add, map, table, t.entity, t.prototype);
			} catch (Exception e) {
				reporting.addException(e);
			}
		});

		renderingTuples.forEach(t -> {
			try {
				t.factory.createWireConnections(renderers::add, map, table, t.entity, t.prototype);
			} catch (Exception e) {
				reporting.addException(e);
			}
		});

		showLogisticGrid(renderers::add, table, map);

		return applyRendering(reporting, (int) Math.round(TypeRendererFactory.tileSize), renderers);
	}

	private static void showLogisticGrid(Consumer<Renderer> register, DataTable table, WorldMap map) {
		Table<Integer, Integer, LogisticGridCell> logisticGrid = map.getLogisticGrid();
		logisticGrid.cellSet().forEach(c -> {
			Point2D.Double pos = new Point2D.Double(c.getRowKey() / 2.0 + 0.25, c.getColumnKey() / 2.0 + 0.25);
			LogisticGridCell cell = c.getValue();
			cell.getTransits().ifPresent(s -> {
				if (s.isEmpty()) {
					return;
				}
				int i = 0;
				float width = 0.3f / s.size();
				for (String itemName : s) {
					double shift = ((i + 1) / (double) (s.size() + 1) - 0.5) / 3.0; // -0.25..0.25
					cell.getMove().filter(d -> map.getLogisticGridCell(d.offset(pos, 0.5))
							.map(LogisticGridCell::isAccepting).orElse(false)).ifPresent(d -> {
								register.accept(new Renderer(Layer.LOGISTICS_MOVE, pos) {
									@Override
									public void render(Graphics2D g) {
										Stroke ps = g.getStroke();
										g.setStroke(
												new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
										g.setColor(Utils.withAlpha(getItemLogisticColor(table, itemName),
												255 - 127 / s.size()));
										g.draw(new Line2D.Double(d.right().offset(pos, shift),
												d.right().offset(d.offset(pos, 0.5), shift)));
										g.setStroke(ps);
									}
								});
							});
					i++;
				}

			});

			if (map.getDebug().logistic) {
				cell.getMovedFrom().ifPresent(l -> {
					for (Direction d : l) {
						Point2D.Double p = d.offset(pos, 0.5);
						register.accept(new Renderer(Layer.DEBUG_LA1, p) {
							@Override
							public void render(Graphics2D g) {
								Stroke ps = g.getStroke();
								g.setStroke(new BasicStroke(2 / 32f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
								g.setColor(Color.cyan);
								g.draw(new Line2D.Double(pos, p));
								g.setStroke(ps);
							}
						});
					}
				});
				cell.getWarpedFrom().ifPresent(l -> {
					for (Point2D.Double p : l) {
						register.accept(new Renderer(Layer.DEBUG_LA2, p) {
							@Override
							public void render(Graphics2D g) {
								Stroke ps = g.getStroke();
								g.setStroke(new BasicStroke(2 / 32f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
								g.setColor(Color.magenta);
								g.draw(new Line2D.Double(pos, p));
								g.setStroke(ps);
							}
						});
					}
				});
			}
		});
	}
}
