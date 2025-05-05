package com.demod.fbsr;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.json.JSONArray;
import org.json.JSONTokener;
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

public class AtlasManager {
	public static final String IMAGE_EXT = "webp";
	public static final String IMAGE_MIME = "image/webp";

	public static class Atlas {
		private final int id;

		private final BufferedImage bufImage;
		private final Quadtree occupied;

//		private VolatileImage volImage;

		public Atlas(int id) {// Generating Atlas
			this.id = id;

			bufImage = new BufferedImage(ATLAS_SIZE, ATLAS_SIZE, BufferedImage.TYPE_INT_ARGB);
			occupied = new Quadtree(0, new Rectangle(0, 0, ATLAS_SIZE, ATLAS_SIZE));
		}

		public Atlas(int id, BufferedImage image) {
			this.id = id;
			bufImage = image;
			occupied = null;
		}

		public int getId() {
			return id;
		}

		public BufferedImage getBufferedImage() {
			return bufImage;
		}
	}

	public static class AtlasRef {
		private boolean valid = false;
		private Atlas atlas = null;
		private Rectangle rect = null;
		private Point trim = null;

		public AtlasRef() {
		}

		public boolean isValid() {
			return valid;
		}

		private void set(Atlas atlas, Rectangle rect, Point trim) {
			valid = true;
			this.atlas = atlas;
			this.rect = rect;
			this.trim = trim;
		}

		private void reset() {
			valid = false;
			atlas = null;
			rect = null;
			trim = null;
		}

		public Atlas getAtlas() {
			return atlas;
		}

		public Rectangle getRect() {
			return rect;
		}

		public Point getTrim() {
			return trim;
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(AtlasManager.class);

	public static int ATLAS_SIZE = 4096;

	private static List<ImageDef> defs = new ArrayList<>();
	private static List<Atlas> atlases = new ArrayList<>();

	private static void copyToAtlas(BufferedImage imageSheet, ImageDef def, Atlas atlas, Rectangle rect) {
		// XXX Inefficient to make a context for every image
		Graphics2D g = atlas.bufImage.createGraphics();
		Rectangle src = def.getTrimmed();
		Rectangle dst = rect;
		g.drawImage(imageSheet, dst.x, dst.y, dst.x + dst.width, dst.y + dst.height, src.x, src.y, src.x + src.width,
				src.y + src.height, null);
		g.dispose();
	}

	public static String computeMD5(BufferedImage imageSheet, Rectangle rect) {
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
		int[] pixels = new int[rect.width * rect.height];
		int span = ret.width;
		imageSheet.getRGB(rect.x, rect.y, rect.width, rect.height, pixels, 0, span);

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

	private static final int MAX_PARALLEL_LOADS = Runtime.getRuntime().availableProcessors() * 2;
	private static final Semaphore loadingSemaphore = new Semaphore(MAX_PARALLEL_LOADS);
	private static final int CLEANUP_INTERVAL = 1000;

	private static JSONArray generateAtlases(File folderAtlas, File fileManifest) throws IOException {
		Map<String, ImageSheetLoader> loaders = new LinkedHashMap<>();
		for (ImageDef def : defs) {
			loaders.put(def.getPath(), def.getLoader());
		}

		LoadingCache<String, BufferedImage> imageSheets = CacheBuilder.newBuilder()
				.softValues()
				.maximumSize(MAX_PARALLEL_LOADS * 100)
				.removalListener(notification -> {
					if (notification.getValue() instanceof BufferedImage) {
						((BufferedImage) notification.getValue()).flush();
					}
				})
				.build(new CacheLoader<String, BufferedImage>() {
					@Override
					public BufferedImage load(String key) throws Exception {
						return loaders.get(key).apply(key);
					}
				});

		LOGGER.info("Trimming Images...");
		AtomicInteger processedCount = new AtomicInteger(0);
		defs.parallelStream().forEach(def -> {
			if (def.isTrimmable()) {
				try {
					loadingSemaphore.acquire();
					BufferedImage imageSheet;
					try {
						imageSheet = imageSheets.get(def.getPath());
						Rectangle trimmed = trimEmptyRect(imageSheet, def.getSource());
						def.setTrimmed(trimmed);
					} finally {
						loadingSemaphore.release();
						if (processedCount.incrementAndGet() % CLEANUP_INTERVAL == 0) {
							imageSheets.cleanUp();
							imageSheets.invalidateAll();
							LOGGER.info("Trimming Images... {}/{}", processedCount.get(), defs.size());
						}
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new RuntimeException("Interrupted while processing images", e);
				} catch (ExecutionException e) {
					e.printStackTrace();
					System.exit(-1);
				}
			} else {
				def.setTrimmed(def.getSource());
			}
		});

		LOGGER.info("Atlas Packing...");
		defs.sort(Comparator.<ImageDef, Integer>comparing(i -> {
			Rectangle r = i.getTrimmed();
			return r.width * r.height;
		}).reversed());

		Map<String, AtlasRef> locationCheck = new HashMap<>();
		Map<String, AtlasRef> md5Check = new HashMap<>();

		long totalPixels = defs.stream().mapToLong(def -> {
			Rectangle r = def.getTrimmed();
			return r.width * r.height;
		}).sum();
		long progressPixels = 0;

		List<Atlas> atlases = new ArrayList<>();
		atlases.add(new Atlas(atlases.size()));
		int imageCount = 0;
		for (ImageDef def : defs) {
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
				def.getAtlasRef().set(cached.atlas, cached.rect, cached.trim);
				continue;
			}

			BufferedImage imageSheet;
			try {
				imageSheet = imageSheets.get(def.getPath());
			} catch (ExecutionException e) {
				e.printStackTrace();
				System.exit(-1);
				return null;
			}
			String md5key = computeMD5(imageSheet, trimmed);
			cached = md5Check.get(md5key);
			if (cached != null) {
				def.getAtlasRef().set(cached.atlas, cached.rect, cached.trim);
				locationCheck.put(locationKey, def.getAtlasRef());
				continue;
			}

			Atlas atlas;
			Rectangle rect = new Rectangle(trimmed.width, trimmed.height);
			nextImage: while (true) {
				for (int i = atlases.size() - 1; i >= 0; i--) {
					atlas = atlases.get(i);
					for (rect.y = 0; rect.y < ATLAS_SIZE - trimmed.height; rect.y++) {
						int nextY = ATLAS_SIZE;
						for (rect.x = 0; rect.x < ATLAS_SIZE - trimmed.width; rect.x++) {
							Rectangle collision = atlas.occupied.insertIfNoCollision(rect);
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
				}
				LOGGER.info("Atlas {} -  {}/{} ({}%)", atlases.size(), imageCount, defs.size(),
						(100 * progressPixels) / totalPixels);
				atlases.add(new Atlas(atlases.size()));
			}

			Point trim = new Point(trimmed.x - source.x, trimmed.y - source.y);
			def.getAtlasRef().set(atlas, rect, trim);
			locationCheck.put(locationKey, def.getAtlasRef());
			md5Check.put(md5key, def.getAtlasRef());
		}

		LOGGER.info("Freeing Image Sheets...");
		imageSheets.invalidateAll();
		imageSheets.cleanUp();

		folderAtlas.mkdirs();
		for (File file : folderAtlas.listFiles()) {
			file.delete();
		}		

		JSONArray jsonManifest = new JSONArray();
		for (ImageDef def : defs) {
			Rectangle source = def.getSource();
			AtlasRef atlasRef = def.getAtlasRef();
			JSONArray jsonEntry = new JSONArray();
			jsonEntry.put(def.getPath());
			jsonEntry.put(source.x);
			jsonEntry.put(source.y);
			jsonEntry.put(source.width);
			jsonEntry.put(source.height);
			jsonEntry.put(atlasRef.atlas.id);
			jsonEntry.put(atlasRef.rect.x);
			jsonEntry.put(atlasRef.rect.y);
			jsonEntry.put(atlasRef.rect.width);
			jsonEntry.put(atlasRef.rect.height);
			jsonEntry.put(atlasRef.trim.x);
			jsonEntry.put(atlasRef.trim.y);
			jsonManifest.put(jsonEntry);
		}

		atlases.parallelStream().forEach(atlas -> {
			File fileAtlas = new File(folderAtlas, "atlas" + atlas.id + "." + IMAGE_EXT);

//			ImageIO.write(atlas.bufImage, IMAGE_EXT, fileAtlas);

			ImageWriter writer = ImageIO.getImageWritersByMIMEType(IMAGE_MIME).next();
			try {
				try (ImageOutputStream ios = ImageIO.createImageOutputStream(fileAtlas)) {
					writer.setOutput(ios);
					WebPWriteParam param = new WebPWriteParam(writer.getLocale());
					param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
					param.setCompressionType(param.getCompressionTypes()[WebPWriteParam.LOSSY_COMPRESSION]);
					param.setCompressionQuality(0.9f);
					writer.write(null, new IIOImage(atlas.bufImage, null, null), param);
				} catch (IOException e) {
					e.printStackTrace();
				}
			} finally {
				writer.dispose();
			}

			LOGGER.info("Write Atlas: {}", fileAtlas.getAbsolutePath());
		});

		Files.write(jsonManifest.toString(2), fileManifest, StandardCharsets.UTF_8);
		LOGGER.info("Write Manifest: {} ({} entries)", fileManifest.getAbsolutePath(), defs.size());

		LOGGER.info("Atlas generation complete.");
		return jsonManifest;
	}

	public static void initialize() throws IOException {
		File folderAtlas = new File(FactorioManager.getFolderDataRoot(), "atlas");
		File fileManifest = new File(folderAtlas, "atlas-manifest.txt");

		JSONArray jsonManifest = null;
		if (!fileManifest.exists() || !checkValidManifest(jsonManifest = readManifest(fileManifest))) {
			jsonManifest = generateAtlases(folderAtlas, fileManifest);

		}

		if (jsonManifest == null) {
			jsonManifest = readManifest(fileManifest);
		}
		loadAtlases(folderAtlas, jsonManifest);
	}

	private static JSONArray readManifest(File fileManifest) throws IOException {
		JSONArray jsonManifest;
		try (FileReader fr = new FileReader(fileManifest)) {
			jsonManifest = new JSONArray(new JSONTokener(fr));
		}
		LOGGER.info("Read Manifest: {} ({} entries)", fileManifest.getAbsolutePath(), jsonManifest.length());
		return jsonManifest;
	}

	private static boolean checkValidManifest(JSONArray jsonManifest) throws IOException {
		Set<String> currentKeys = new HashSet<>();
		for (ImageDef image : defs) {
			Rectangle source = image.getSource();
			String locationKey = image.getPath() + "|" + source.x + "|" + source.y + "|" + source.width + "|"
					+ source.height;
			currentKeys.add(locationKey);
		}

		Set<String> manifestKeys = new HashSet<>();

		for (int i = 0; i < jsonManifest.length(); i++) {
			JSONArray jsonEntry = jsonManifest.getJSONArray(i);
			String path = jsonEntry.getString(0);
			int srcX = jsonEntry.getInt(1);
			int srcY = jsonEntry.getInt(2);
			int width = jsonEntry.getInt(3);
			int height = jsonEntry.getInt(4);
			String locationKey = path + "|" + srcX + "|" + srcY + "|" + width + "|" + height;
			manifestKeys.add(locationKey);
		}

		SetView<String> mismatched = Sets.symmetricDifference(currentKeys, manifestKeys);

		if (!mismatched.isEmpty()) {
			LOGGER.error("Atlas manifest mismatch detected: {} keys are different", mismatched.size());
		}

		return mismatched.isEmpty();
	}

	private static void loadAtlases(File folderAtlas, JSONArray jsonManifest) throws IOException {

		defs.forEach(d -> d.getAtlasRef().reset());

		int[] atlasIds = IntStream.range(0, jsonManifest.length()).map(i -> jsonManifest.getJSONArray(i).getInt(5))
				.distinct().toArray();
		atlases = Arrays.stream(atlasIds).parallel().mapToObj(id -> {
			try {
				File fileAtlas = new File(folderAtlas, "atlas" + id + "." + IMAGE_EXT);
				BufferedImage image = ImageIO.read(fileAtlas);
				LOGGER.info("Read Atlas: {}", fileAtlas.getAbsolutePath());
				return new Atlas(id, image);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1);
				return null;
			}
		}).sorted(Comparator.comparing(a -> a.getId())).collect(Collectors.toList());

		// XXX not an elegant approach
		class RefValues {
			Atlas atlas;
			Rectangle rect;
			Point trim;
		}
		Map<String, RefValues> locationMap = new HashMap<>();
		for (int i = 0; i < jsonManifest.length(); i++) {
			JSONArray jsonEntry = jsonManifest.getJSONArray(i);
			String path = jsonEntry.getString(0);
			int srcX = jsonEntry.getInt(1);
			int srcY = jsonEntry.getInt(2);
			int srcWidth = jsonEntry.getInt(3);
			int srcHeight = jsonEntry.getInt(4);
			int id = jsonEntry.getInt(5);
			int atlasX = jsonEntry.getInt(6);
			int atlasY = jsonEntry.getInt(7);
			int atlasWidth = jsonEntry.getInt(8);
			int atlasHeight = jsonEntry.getInt(9);
			int trimX = jsonEntry.getInt(10);
			int trimY = jsonEntry.getInt(11);
			String locationKey = path + "|" + srcX + "|" + srcY + "|" + srcWidth + "|" + srcHeight;
			RefValues ref = new RefValues();
			ref.atlas = atlases.get(id);
			ref.rect = new Rectangle(atlasX, atlasY, atlasWidth, atlasHeight);
			ref.trim = new Point(trimX, trimY);
			locationMap.put(locationKey, ref);
		}

		for (ImageDef image : defs) {
			Rectangle source = image.getSource();
			String locationKey = image.getPath() + "|" + source.x + "|" + source.y + "|" + source.width + "|"
					+ source.height;
			RefValues ref = locationMap.get(locationKey);
			if (ref == null) {
				LOGGER.error("MISSING ATLAS ENTRY FOR {}", locationKey);
			} else {
				image.getAtlasRef().set(ref.atlas, ref.rect, ref.trim);
				image.setTrimmed(
						new Rectangle(source.x + ref.trim.x, source.y + ref.trim.y, ref.rect.width, ref.rect.height));
			}
		}

	}

	public static void registerDef(ImageDef def) {
		if (def == null) {
			throw new NullPointerException("def is null!");
		}
		defs.add(def);
	}
}
