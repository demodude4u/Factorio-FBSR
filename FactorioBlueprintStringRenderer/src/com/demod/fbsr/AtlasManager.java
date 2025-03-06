package com.demod.fbsr;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.VolatileImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.json.JSONArray;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.io.Files;

public class AtlasManager {
	public static class Atlas {
		private final int id;
		private final BufferedImage bufImage;
		private final Quadtree occupied;

//		private VolatileImage volImage;

		public Atlas(int id) {// Generating Atlas
			this.id = id;
			bufImage = new BufferedImage(ATLAS_SIZE, ATLAS_SIZE, BufferedImage.TYPE_INT_ARGB_PRE);
//			volImage = null;
			occupied = new Quadtree(0, new Rectangle(0, 0, ATLAS_SIZE, ATLAS_SIZE));
		}

		public Atlas(int id, BufferedImage image) {
			this.id = id;
			bufImage = image;
//			volImage = null;
			occupied = null;
		}

		public int getId() {
			return id;
		}

		public BufferedImage getBufferedImage() {
			return bufImage;
		}

//		public void refreshVolatileImage() {
//			GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
//					.getDefaultConfiguration();
//
//			if (volImage == null) {
//				volImage = gc.createCompatibleVolatileImage(bufImage.getWidth(), bufImage.getHeight(),
//						Transparency.TRANSLUCENT);
//				Graphics2D g = volImage.createGraphics();
//				g.drawImage(bufImage, 0, 0, null);
//				g.dispose();
//			}
//
//			while (true) {
//				int validStatus = volImage.validate(gc);
//
//				if (validStatus == VolatileImage.IMAGE_INCOMPATIBLE) {
//					volImage = gc.createCompatibleVolatileImage(bufImage.getWidth(), bufImage.getHeight(),
//							Transparency.TRANSLUCENT);
//				}
//
//				if (volImage.contentsLost() || validStatus == VolatileImage.IMAGE_INCOMPATIBLE) {
//					Graphics2D g = volImage.createGraphics();
//					g.drawImage(bufImage, 0, 0, null);
//					g.dispose();
//					continue;
//				}
//
//				break;
//			}
//		}
//
//		public VolatileImage getVolatileImage() {
//			return volImage;
//		}
	}

	public static class AtlasRef {
		private boolean valid = false;
		private Atlas atlas = null;
		private Rectangle rect = null;

		public AtlasRef() {
		}

		public boolean isValid() {
			return valid;
		}

		private void set(Atlas atlas, Rectangle rect) {
			valid = true;
			this.atlas = atlas;
			this.rect = rect;
		}

		public Atlas getAtlas() {
			return atlas;
		}

		public Rectangle getRect() {
			return rect;
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(AtlasManager.class);

	public static int ATLAS_SIZE = 4096;

	private static List<ImageDef> defs = new ArrayList<>();
	private static List<Atlas> atlases = new ArrayList<>();

	private static void copyToAtlas(ImageDef def, Atlas atlas, Rectangle rect) {
		// XXX Inefficient to make a context for every image
		Graphics2D g = atlas.bufImage.createGraphics();
		BufferedImage image = FactorioManager.lookupModImage(def.path);
		Rectangle src = def.source;
		Rectangle dst = rect;
		g.drawImage(image, dst.x, dst.y, dst.x + dst.width, dst.y + dst.height, src.x, src.y, src.x + src.width,
				src.y + src.height, null);
		g.dispose();
	}

	public static String computeMD5(ImageDef def) {
		BufferedImage imageSheet = FactorioManager.lookupModImage(def.path);
		int width = def.source.width;
		int height = def.source.height;
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
		Graphics2D g = image.createGraphics();
		int x = def.source.x;
		int y = def.source.y;
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

	private static void generateAtlases(File folderAtlas, File fileManifest) throws IOException {
		if (!atlases.isEmpty()) {
			throw new IllegalStateException("Atlases are already generated!");
		}

		Map<String, AtlasRef> locationCheck = new HashMap<>();
		Map<String, AtlasRef> md5Check = new HashMap<>();

		defs.sort(Comparator.<ImageDef, Integer>comparing(i -> {
			Rectangle r = i.getSource();
			return r.width * r.height;
		}).reversed());

		atlases.add(new Atlas(0));
		int imageCount = 0;
		for (ImageDef image : defs) {
			imageCount++;

			if (image.getAtlasRef().isValid()) {
				continue;// Shared ref
			}

			Rectangle source = image.source;
			String locationKey = image.path + "|" + source.x + "|" + source.y + "|" + source.width + "|"
					+ source.height;

			AtlasRef cached = locationCheck.get(locationKey);
			if (cached != null) {
				image.getAtlasRef().set(cached.atlas, cached.rect);
				continue;
			}

			String md5key = computeMD5(image);
			cached = md5Check.get(md5key);
			if (cached != null) {
				image.getAtlasRef().set(cached.atlas, cached.rect);
				locationCheck.put(locationKey, image.getAtlasRef());
				continue;
			}

			Atlas atlas;
			Rectangle rect = new Rectangle(source.width, source.height);
			nextImage: while (true) {
				for (int i = atlases.size() - 1; i >= 0; i--) {
					atlas = atlases.get(i);
					for (rect.y = 0; rect.y < ATLAS_SIZE - source.height; rect.y++) {
						int nextY = ATLAS_SIZE;
						for (rect.x = 0; rect.x < ATLAS_SIZE - source.width; rect.x++) {
							Rectangle collision = atlas.occupied.insertIfNoCollision(rect);
							if (collision != null) {
								rect.x = collision.x + collision.width - 1;
								nextY = Math.min(nextY, collision.y + collision.height);
							} else {
								copyToAtlas(image, atlas, rect);
								break nextImage;
							}
						}
						rect.y = nextY - 1;
					}
				}
				LOGGER.info("Atlas {} -  {}/{} ({}%, area {}px)", atlases.size(), imageCount, defs.size(),
						(100 * imageCount) / defs.size(), rect.width * rect.height);
				atlases.add(new Atlas(atlases.size()));
			}

			image.getAtlasRef().set(atlas, rect);
			locationCheck.put(locationKey, image.getAtlasRef());
			md5Check.put(md5key, image.getAtlasRef());
		}

		folderAtlas.mkdirs();
		for (File file : folderAtlas.listFiles()) {
			file.delete();
		}

		JSONArray jsonManifest = new JSONArray();
		for (ImageDef def : defs) {
			JSONArray jsonEntry = new JSONArray();
			jsonEntry.put(def.path);
			jsonEntry.put(def.source.x);
			jsonEntry.put(def.source.y);
			jsonEntry.put(def.source.width);
			jsonEntry.put(def.source.height);
			jsonEntry.put(def.atlasRef.atlas.id);
			jsonEntry.put(def.atlasRef.rect.x);
			jsonEntry.put(def.atlasRef.rect.y);
			jsonManifest.put(jsonEntry);
		}
		Files.write(jsonManifest.toString(2), fileManifest, StandardCharsets.UTF_8);
		LOGGER.info("Write Manifest: {} ({} entries)", fileManifest.getAbsolutePath(), defs.size());

		for (Atlas atlas : atlases) {
			File fileAtlas = new File(folderAtlas, "atlas" + atlas.id + ".png");
			ImageIO.write(atlas.bufImage, "PNG", fileAtlas);
			LOGGER.info("Write Atlas: {}", fileAtlas.getAbsolutePath());
		}

		LOGGER.info("Atlas generation complete.");
	}

	public static void initialize() throws IOException {
		File folderAtlas = new File(FactorioManager.getFolderDataRoot(), "atlas");
		File fileManifest = new File(folderAtlas, "atlas-manifest.txt");

		if (fileManifest.exists() && checkValidManifest(fileManifest)) {
			loadAtlases(folderAtlas, fileManifest);
		} else {
			generateAtlases(folderAtlas, fileManifest);
		}
	}

	private static boolean checkValidManifest(File fileManifest) throws IOException {
		Set<String> currentKeys = new HashSet<>();
		for (ImageDef image : defs) {
			Rectangle source = image.source;
			String locationKey = image.path + "|" + source.x + "|" + source.y + "|" + source.width + "|"
					+ source.height;
			currentKeys.add(locationKey);
		}

		Set<String> manifestKeys = new HashSet<>();
		JSONArray jsonManifest;
		try (FileReader fr = new FileReader(fileManifest)) {
			jsonManifest = new JSONArray(new JSONTokener(fr));
		}
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
			LOGGER.error("ATLAS MANIFEST MISMATCHED KEYS:\n{}",
					mismatched.stream().sorted().collect(Collectors.joining("\n")));
		}

		return mismatched.isEmpty();
	}

	private static void loadAtlases(File folderAtlas, File fileManifest) throws IOException {
		JSONArray jsonManifest;
		try (FileReader fr = new FileReader(fileManifest)) {
			jsonManifest = new JSONArray(new JSONTokener(fr));
		}
		LOGGER.info("Read Manifest: {} ({} entries)", fileManifest.getAbsolutePath(), jsonManifest.length());

		{
			int id = 0;
			File fileAtlas;
			while ((fileAtlas = new File(folderAtlas, "atlas" + id + ".png")).exists()) {
				BufferedImage image = ImageIO.read(fileAtlas);
				Atlas atlas = new Atlas(id++, image);
				atlases.add(atlas);
				LOGGER.info("Read Atlas: {}", fileAtlas.getAbsolutePath());
			}
		}

		// XXX not an elegant approach
		class RefValues {
			Atlas atlas;
			Rectangle rect;
		}
		Map<String, RefValues> locationMap = new HashMap<>();
		for (int i = 0; i < jsonManifest.length(); i++) {
			JSONArray jsonEntry = jsonManifest.getJSONArray(i);
			String path = jsonEntry.getString(0);
			int srcX = jsonEntry.getInt(1);
			int srcY = jsonEntry.getInt(2);
			int width = jsonEntry.getInt(3);
			int height = jsonEntry.getInt(4);
			int id = jsonEntry.getInt(5);
			int atlasX = jsonEntry.getInt(6);
			int atlasY = jsonEntry.getInt(7);
			String locationKey = path + "|" + srcX + "|" + srcY + "|" + width + "|" + height;
			RefValues ref = new RefValues();
			ref.atlas = atlases.get(id);
			ref.rect = new Rectangle(atlasX, atlasY, width, height);
			locationMap.put(locationKey, ref);
		}

		for (ImageDef image : defs) {
			Rectangle source = image.source;
			String locationKey = image.path + "|" + source.x + "|" + source.y + "|" + source.width + "|"
					+ source.height;
			RefValues ref = locationMap.get(locationKey);
			if (ref == null) {
				LOGGER.error("MISSING ATLAS ENTRY FOR {}", locationKey);
			} else {
				image.getAtlasRef().set(ref.atlas, ref.rect);
			}
		}

	}

	public static void registerDef(ImageDef def) {
		defs.add(def);
	}

//	public static void refreshVolatileBuffers() {
//		for (Atlas atlas : atlases) {
//			atlas.refreshVolatileImage();
//		}
//	}
}
