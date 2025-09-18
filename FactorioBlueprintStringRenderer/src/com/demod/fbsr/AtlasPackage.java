package com.demod.fbsr;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.rapidoid.commons.Arr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.ImageDef.ImageSheetLoader;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.io.Files;
import com.luciad.imageio.webp.WebPWriteParam;

public class AtlasPackage {
	private static final Logger LOGGER = LoggerFactory.getLogger(AtlasPackage.class);

	public static int ATLAS_SIZE = 4096;
	public static int ATLAS_ICONS_SIZE = 2048;

	private List<ImageDef> defs = new ArrayList<>();

	private final AtlasManager atlasManager;
	private final boolean alwaysLoaded;
	private final int id;
	
	private File fileAssetsZip = null;
	private int atlasCount = 0;

	public AtlasPackage(AtlasManager atlasManager, boolean alwaysLoaded) {
		this.atlasManager = atlasManager;
		this.alwaysLoaded = alwaysLoaded;

		id = atlasManager.register(this);
	}

	public AtlasManager getAtlasManager() {
		return atlasManager;
	}

	public boolean isAlwaysLoaded() {
		return alwaysLoaded;
	}

	public int getId() {
		return id;
	}

	public JSONObject populateZip(ZipOutputStream zos) throws IOException {
		for (ImageDef def : defs) {
			def.getAtlasRef().reset();
		}

		System.gc();

		class DefGroup {
			String key;
			ImageSheetLoader loader;
			List<ImageDef> defs;
			int maxPixels;
		}
		
		List<DefGroup> defGroups = defs.stream().collect(Collectors.groupingBy(def -> def.getPath()))
				.entrySet().stream().map(e -> {
					DefGroup g = new DefGroup();
					g.key = e.getKey();
					g.loader = e.getValue().get(0).getLoader();
					g.defs = new ArrayList<>(e.getValue());
					return g;
				}).collect(Collectors.toList());
		LOGGER.info("Def Groups: {}", defGroups.size());
		LOGGER.info("Def Count: {}", defs.size());

		LOGGER.info("Trimming Images...");
		AtomicInteger processedCount = new AtomicInteger(0);
		int updateInterval = 1000;
		defGroups.parallelStream().forEach(g -> {
			List<ImageDef> group = g.defs;
			if (group.stream().noneMatch(ImageDef::isTrimmable)) {
				group.forEach(def -> def.setTrimmed(def.getSource()));
				return;
			}

			BufferedImage imageSheet = g.loader.apply(g.key);

			group.parallelStream().forEach(def -> {
				if ((processedCount.incrementAndGet() % updateInterval) == 0) {
					LOGGER.info("Trimming Images... {}/{}", processedCount.get(), defs.size());
				}
				
				if (!def.isTrimmable()) {
					def.setTrimmed(def.getSource());
					return;
				}
				
				def.setTrimmed(trimEmptyRect(imageSheet, def.getSource()));
			});
		});

		LOGGER.info("Atlas Packing...");
		for (DefGroup g : defGroups) {
			g.defs.sort(Comparator.<ImageDef, Integer>comparing(d -> d.getTrimmed().width * d.getTrimmed().height).reversed());
			Rectangle trimmed = g.defs.get(0).getTrimmed();
			g.maxPixels = trimmed.width * trimmed.height;
		}
		defGroups.sort(Comparator.<DefGroup, Integer>comparing(g -> {
			return g.maxPixels;
		}).reversed());

		Map<String, AtlasRef> locationCheck = new HashMap<>();
		Map<String, AtlasRef> md5Check = new HashMap<>();

		long totalPixels = defs.stream().mapToLong(def -> {
			Rectangle r = def.getTrimmed();
			return r.width * r.height;
		}).sum();
		long progressPixels = 0;

		List<AtlasBuilder> atlases = new ArrayList<>();
		atlases.add(AtlasBuilder.init(this, atlases.size(), ATLAS_SIZE, ATLAS_SIZE));
		AtlasBuilder iconsAtlas = null;
		int imageCount = 0;
		for (DefGroup group : defGroups) {
			BufferedImage imageSheet = group.loader.apply(group.key);

			for (ImageDef def : group.defs) {
				imageCount++;
	
				if (def.getAtlasRef().isValid()) {
					continue;// Shared ref
				}
	
				Rectangle source = def.getSource();
				Rectangle trimmed = def.getTrimmed();
				progressPixels += trimmed.width * trimmed.height;
	
				String locationKey = def.getPath() + "|" + source.x + "|" + source.y + "|" + source.width + "|"
						+ source.height;
	
				AtlasRef cached = locationCheck.get(locationKey);
				if (cached != null) {
					def.getAtlasRef().link(cached);
					continue;
				}

				String md5key = computeMD5(imageSheet, trimmed);
				cached = md5Check.get(md5key);
				if (cached != null) {
					def.getAtlasRef().link(cached);
					locationCheck.put(locationKey, def.getAtlasRef());
					continue;
				}
	
				Rectangle rect = new Rectangle(trimmed.width, trimmed.height);
				boolean icon = (rect.width <= IconManager.ICON_SIZE)  && (rect.height <= IconManager.ICON_SIZE);
	
				AtlasBuilder atlas;
				if (icon) {
					if (iconsAtlas == null || (iconsAtlas.getIconCount() >= iconsAtlas.getIconMaxCount())) {
						iconsAtlas = AtlasBuilder.initIcons(this, atlases.size(), ATLAS_ICONS_SIZE, ATLAS_ICONS_SIZE, IconManager.ICON_SIZE);
						LOGGER.info("Icons Atlas {} -  {}/{} ({}%)", atlases.size(), imageCount, defs.size(),
								(100 * progressPixels) / totalPixels);
						atlases.add(iconsAtlas);
					}
					atlas = iconsAtlas;
					int iconCount = atlas.getIconCount();
					int iconColumns = atlas.getIconColumns();
					int iconSize = atlas.getIconSize();
					rect.x = (iconCount % iconColumns) * iconSize;
					rect.y = (iconCount / iconColumns) * iconSize;
					copyToAtlas(imageSheet, def, atlas, rect);
					atlas.setIconCount(iconCount + 1);
	
				} else {
					nextImage: while (true) {
						nextAtlas: for (int id = atlases.size() - 1; id >= 0; id--) {
							atlas = atlases.get(id);
							if (atlas.isIconMode()) {
								continue;
							}
	
							List<Dimension> failedPackingSizes = atlas.getFailedPackingSizes();
							for (Dimension size : failedPackingSizes) {
								if (rect.width >= size.width && rect.height >= size.height) {
									continue nextAtlas;
								}
							}
	
							Quadtree occupied = atlas.getOccupied();
							for (rect.y = 0; rect.y < ATLAS_SIZE - rect.height; rect.y++) {
								int nextY = ATLAS_SIZE;
								for (rect.x = 0; rect.x < ATLAS_SIZE - rect.width; rect.x++) {
									Rectangle collision = occupied.insertIfNoCollision(rect);
									if (collision != null) {
										rect.x = collision.x + collision.width - 1;
										nextY = Math.min(nextY, collision.y + collision.height);
									} else {
										copyToAtlas(imageSheet, def, atlas, rect);
										break nextImage;
									}
								}
								rect.y = nextY - 1;
							}
	
							{
								boolean replaced = false;
								for (Dimension size : failedPackingSizes) {
									if (rect.width <= size.width && rect.height <= size.height) {
										size.setSize(rect.width, rect.height);
										replaced = true;
										break;
									}
								}
								if (!replaced) {
									failedPackingSizes.add(new Dimension(rect.width, rect.height));
								}
							}
						}
						LOGGER.info("Atlas {} -  {}/{} ({}%)", atlases.size(), imageCount, defs.size(),
								(100 * progressPixels) / totalPixels);
						atlases.add(AtlasBuilder.init(this, atlases.size(), ATLAS_SIZE, ATLAS_SIZE));
					}
				}
	
				Point trim = new Point(trimmed.x - source.x, trimmed.y - source.y);
				def.getAtlasRef().set(id, atlas.getId(), rect, trim);
				locationCheck.put(locationKey, def.getAtlasRef());
				md5Check.put(md5key, def.getAtlasRef());
			}
		}

		for (AtlasBuilder atlas : atlases) {
			atlas.getGraphics().dispose();
		}

		JSONObject jsonManifest = new JSONObject();
		JSONArray jsonEntries = new JSONArray();
		jsonManifest.put("entries", jsonEntries);
		for (ImageDef def : defs) {
			Rectangle source = def.getSource();
			AtlasRef atlasRef = def.getAtlasRef();
			JSONArray jsonEntry = new JSONArray();
			AtlasBuilder atlas = atlases.get(atlasRef.getAtlasId());
			Rectangle rect = atlasRef.getRect();
			Point trim = atlasRef.getTrim();

			if (atlas.getAtlasPackage() != this) {
				LOGGER.error("Image does not belong to this atlas package: {}", def.getPath());
				System.exit(-1);
			}

			jsonEntry.put(def.getPath());
			jsonEntry.put(source.x);
			jsonEntry.put(source.y);
			jsonEntry.put(source.width);
			jsonEntry.put(source.height);
			jsonEntry.put(atlas.getId());
			jsonEntry.put(rect.x);
			jsonEntry.put(rect.y);
			jsonEntry.put(rect.width);
			jsonEntry.put(rect.height);
			jsonEntry.put(trim.x);
			jsonEntry.put(trim.y);
			jsonEntries.put(jsonEntry);
		}

		for (AtlasBuilder atlas : atlases) {
			String filename = "atlas" + atlas.getId() + ".webp";
			zos.putNextEntry(new ZipEntry(filename));

			ImageWriter writer = ImageIO.getImageWritersByMIMEType("image/webp").next();
			try {
				try (ImageOutputStream ios = ImageIO.createImageOutputStream(zos)) {
					writer.setOutput(ios);
					WebPWriteParam param = new WebPWriteParam(writer.getLocale());
					param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
					param.setCompressionType(param.getCompressionTypes()[WebPWriteParam.LOSSY_COMPRESSION]);
					param.setCompressionQuality(0.9f);
					writer.write(null, new IIOImage(atlas.getImage(), null, null), param);
				} catch (IOException e) {
					e.printStackTrace();
				}
			} finally {
				writer.dispose();
			}

			zos.closeEntry();
			LOGGER.info("Write Atlas: {}", filename);
		}

		return jsonManifest;
	}

	public void setZip(File fileAssetsZip) {
		this.fileAssetsZip = fileAssetsZip;
	}

    public boolean loadManifest(JSONObject jsonManifest) {
		if (!checkValidManifest(jsonManifest)) {
			System.out.println("Atlas manifest is invalid or does not match current definitions. Assets need to be regenerated.");
			return false;
		}
			
		JSONArray jsonEntries = jsonManifest.getJSONArray("entries");

		defs.forEach(d -> d.getAtlasRef().reset());

		atlasCount = (int) IntStream.range(0, jsonEntries.length()).mapToLong(i -> jsonEntries.getJSONArray(i).getInt(5)).max().orElse(0) + 1;

		Map<String, AtlasRef> locationMap = new HashMap<>();
		for (int i = 0; i < jsonEntries.length(); i++) {
			JSONArray jsonEntry = jsonEntries.getJSONArray(i);
			String path = jsonEntry.getString(0);
			int srcX = jsonEntry.getInt(1);
			int srcY = jsonEntry.getInt(2);
			int srcWidth = jsonEntry.getInt(3);
			int srcHeight = jsonEntry.getInt(4);
			int atlasId = jsonEntry.getInt(5);
			int atlasX = jsonEntry.getInt(6);
			int atlasY = jsonEntry.getInt(7);
			int atlasWidth = jsonEntry.getInt(8);
			int atlasHeight = jsonEntry.getInt(9);
			int trimX = jsonEntry.getInt(10);
			int trimY = jsonEntry.getInt(11);
			String locationKey = path + "|" + srcX + "|" + srcY + "|" + srcWidth + "|" + srcHeight;
			AtlasRef ref = new AtlasRef();
			Rectangle rect = new Rectangle(atlasX, atlasY, atlasWidth, atlasHeight);
			Point trim = new Point(trimX, trimY);
			ref.set(id, atlasId, rect, trim);
			locationMap.put(locationKey, ref);
		}

		for (ImageDef image : defs) {
			Rectangle source = image.getSource();
			String locationKey = image.getPath() + "|" + source.x + "|" + source.y + "|" + source.width + "|"
					+ source.height;
			AtlasRef ref = locationMap.get(locationKey);
			if (ref == null) {
				LOGGER.error("MISSING ATLAS ENTRY FOR {}", locationKey);
			} else {
				image.getAtlasRef().link(ref);
				Rectangle rect = ref.getRect();
				Point trim = ref.getTrim();
				Rectangle trimmed = new Rectangle(source.x + trim.x, source.y + trim.y, rect.width, rect.height);
				image.setTrimmed(trimmed);
			}
		}

		return true;
	}

	private boolean checkValidManifest(JSONObject jsonManifest) {
		if (!jsonManifest.has("entries")) {
			LOGGER.error("Atlas manifest is missing 'entries' key");
			return false;
		}
		JSONArray jsonEntries = jsonManifest.getJSONArray("entries");

		Set<String> currentKeys = new HashSet<>();
		for (ImageDef image : defs) {
			Rectangle source = image.getSource();
			String locationKey = image.getPath() + "|" + source.x + "|" + source.y + "|" + source.width + "|"
					+ source.height;
			currentKeys.add(locationKey);
		}

		Set<String> manifestKeys = new HashSet<>();

		for (int i = 0; i < jsonEntries.length(); i++) {
			JSONArray jsonEntry = jsonEntries.getJSONArray(i);
			String path = jsonEntry.getString(0);
			int srcX = jsonEntry.getInt(1);
			int srcY = jsonEntry.getInt(2);
			int width = jsonEntry.getInt(3);
			int height = jsonEntry.getInt(4);
			String locationKey = path + "|" + srcX + "|" + srcY + "|" + width + "|" + height;
			manifestKeys.add(locationKey);
		}

		SetView<String> mismatched = Sets.difference(currentKeys, manifestKeys);

		if (!mismatched.isEmpty()) {
			LOGGER.error("Atlas manifest mismatch detected: {} keys are different", mismatched.size());
			int count = 0;
			for (String key : mismatched.stream().sorted().collect(Collectors.toList())) {
				if (currentKeys.contains(key)) {
					LOGGER.error("Missing in manifest: {}", key);
				} else {
					LOGGER.error("Extra in manifest: {}", key);
				}
				if (++count >= 10) {
					LOGGER.error("... and {} more", mismatched.size() - count);
					break;
				}
			}
		}

		return mismatched.isEmpty();
	}

	public BufferedImage[] readAtlases(int... atlasIds) {
		if (atlasIds.length == 0) {
			return new BufferedImage[0];
		}

		LOGGER.info("Reading {} atlas {}", fileAssetsZip.getName(), Arrays.toString(atlasIds));

		BufferedImage[] images = new BufferedImage[atlasIds.length];
		try (ZipFile zip = new ZipFile(fileAssetsZip)) {
			for (int i = 0; i < atlasIds.length; i++) {
				int atlasId = atlasIds[i];
				ZipEntry entry = zip.getEntry("atlas" + atlasId + ".webp");
				try (InputStream is = zip.getInputStream(entry)) {
					images[i] = ImageIO.read(is);
				}
			}
			return images;
		} catch (OutOfMemoryError e) {
			System.out.println("///////////////////////////////////////////");
			System.out.println("///////////////////////////////////////////");
			System.out.println("///// OUT OF MEMORY ///////////////////////");
			System.out.println("///////////////////////////////////////////");
			System.out.println("///////////////////////////////////////////");
			try {
				System.in.read();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			System.exit(-1);
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
			return null;
		}
	}

	public void registerDef(ImageDef def) {
		if (def == null) {
			throw new NullPointerException("def is null!");
		}
		defs.add(def);
	}

	public List<ImageDef> getDefs() {
		return defs;
	}

	public int getAtlasCount() {
		return atlasCount;
	}

    private static void copyToAtlas(BufferedImage imageSheet, ImageDef def, AtlasBuilder atlas, Rectangle rect) {
		Rectangle src = def.getTrimmed();
		Rectangle dst = rect;
		atlas.getGraphics().drawImage(imageSheet, dst.x, dst.y, dst.x + dst.width, dst.y + dst.height, src.x, src.y, src.x + src.width,
				src.y + src.height, null);
	}

    private static String computeMD5(BufferedImage imageSheet, Rectangle rect) {
		int x = rect.x;
		int y = rect.y;
		int width = rect.width;
		int height = rect.height;
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
		Graphics2D g = image.createGraphics();
		g.drawImage(imageSheet, 0, 0, width, height, x, y, x + width, y + height, null);
		g.dispose();
		byte[] imageBytes = extractPixelData(image);
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			System.exit(-1);
			return null;
		}
		byte[] digest = md.digest(imageBytes);
		StringBuilder sb = new StringBuilder();
		for (byte b : digest) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

    private static byte[] extractPixelData(BufferedImage image) {
		if (image.getRaster().getDataBuffer() instanceof DataBufferByte) {
			// Fastest for images with a direct byte buffer
			return ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		} else if (image.getRaster().getDataBuffer() instanceof DataBufferInt) {
			// Convert int[] to byte[]
			int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			for (int pixel : pixels) {
				baos.write((pixel >> 24) & 0xFF);
				baos.write((pixel >> 16) & 0xFF);
				baos.write((pixel >> 8) & 0xFF);
				baos.write(pixel & 0xFF);
			}
			return baos.toByteArray();
		} else {
			// Fallback for images that require conversion
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				ImageIO.write(image, "png", baos);
			} catch (Exception e) {
				throw new RuntimeException("Error encoding image", e);
			}
			return baos.toByteArray();
		}
	}

    private static Rectangle trimEmptyRect(BufferedImage imageSheet, Rectangle rect) {
		Rectangle ret = new Rectangle(rect.width, rect.height);
		
		//Rare modding issue
		if (ret.width > imageSheet.getWidth()) {
			ret.width = imageSheet.getWidth();
		}
		if (ret.height > imageSheet.getHeight()) {
			ret.height = imageSheet.getHeight();
		}
		
		int[] pixels = new int[ret.width * ret.height];
		int span = ret.width;
		imageSheet.getRGB(rect.x, rect.y, ret.width, ret.height, pixels, 0, span);

		// Top
		boolean fullEmpty = true;
		scan: for (int y = ret.y, yEnd = y + ret.height; y < yEnd; y++) {
			for (int x = ret.x, xEnd = x + ret.width; x < xEnd; x++) {
				if (((pixels[y * span + x] >> 24) & 0xFF) > 0) {
					int trim = y - ret.y;
					ret.y += trim;
					ret.height -= trim;
					fullEmpty = false;
					break scan;
				}
			}
		}
		if (fullEmpty) { // 1x1 transparent
			ret.width = 1;
			ret.height = 1;
			ret.x += rect.x;
			ret.y += rect.y;
			return ret;
		}

		// Bottom
		scan: for (int yEnd = ret.y, y = yEnd + ret.height - 1; y >= yEnd; y--) {
			for (int x = ret.x, xEnd = x + ret.width; x < xEnd; x++) {
				if (((pixels[y * span + x] >> 24) & 0xFF) > 0) {
					int trim = (ret.y + ret.height - 1) - y;
					ret.height -= trim;
					break scan;
				}
			}
		}

		// Left
		scan: for (int x = ret.x, xEnd = x + ret.width; x < xEnd; x++) {
			for (int y = ret.y, yEnd = y + ret.height; y < yEnd; y++) {
				if (((pixels[y * span + x] >> 24) & 0xFF) > 0) {
					int trim = x - ret.x;
					ret.x += trim;
					ret.width -= trim;
					break scan;
				}
			}
		}

		// Right
		scan: for (int xEnd = ret.x, x = xEnd + ret.width - 1; x >= xEnd; x--) {
			for (int y = ret.y, yEnd = y + ret.height; y < yEnd; y++) {
				if (((pixels[y * span + x] >> 24) & 0xFF) > 0) {
					int trim = (ret.x + ret.width - 1) - x;
					ret.width -= trim;
					break scan;
				}
			}
		}

		ret.x += rect.x;
		ret.y += rect.y;
		return ret;
	}
}
