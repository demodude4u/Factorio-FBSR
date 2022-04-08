package com.demod.fbsr;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Hex;
import org.dizitart.no2.Document;
import org.dizitart.no2.IndexOptions;
import org.dizitart.no2.IndexType;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.NitriteCollection;
import org.dizitart.no2.filters.Filters;
import org.json.JSONException;
import org.json.JSONObject;

import com.demod.factorio.Utils;
import com.demod.fbsr.app.BlueprintBotDiscordService;
import com.demod.fbsr.app.ServiceFinder;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

public final class WebUtils {
	private static Nitrite db = initializeDatabase();
	private static NitriteCollection dbUploads = db.getCollection("uploads");

	public static void addPossiblyLargeEmbedField(EmbedBuilder builder, String name, String value, boolean inline)
			throws IOException {
		if (value.length() <= MessageEmbed.VALUE_MAX_LENGTH) {
			builder.addField(name, value, inline);
		} else {
			builder.addField(name + " Link", uploadToHostingService(name + ".txt", value.getBytes()).toString(),
					inline);
		}
	}

	private static void addToUploadedDatabase(String fileHash, String url) {
		dbUploads.insert(Document.createDocument("hash", fileHash).put("url", url));
	}

	private static Optional<String> checkIfUploadedAlready(String fileHash) {
		return Optional.ofNullable(dbUploads.find(Filters.eq("hash", fileHash)).firstOrDefault())
				.flatMap(d -> Optional.ofNullable(d.get("url").toString()));
	}

	private static byte[] generateDiscordFriendlyPNGImage(BufferedImage image) {
		byte[] imageData = WebUtils.getImageData(image);

		while (imageData.length > Message.MAX_FILE_SIZE) {
			image = RenderUtils.scaleImage(image, image.getWidth() / 2, image.getHeight() / 2);
			imageData = WebUtils.getImageData(image);
		}

		return imageData;
	}

	private static String generateFileHash(byte[] fileData) {
		try {
			return Hex.encodeHexString(MessageDigest.getInstance("MD5").digest(fileData));
		} catch (NoSuchAlgorithmException e) {
			throw new InternalError(e);
		}

	}

	public static byte[] getImageData(BufferedImage image) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ImageIO.write(image, "PNG", baos);
			return baos.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
			throw new InternalError(e);
		}
	}

	private static Nitrite initializeDatabase() {
		try {
			Nitrite db = Nitrite.builder().compressed().filePath("database.db").openOrCreate();

			NitriteCollection dbUploads = db.getCollection("uploads");
			if (!dbUploads.hasIndex("hash")) {
				dbUploads.createIndex("hash", IndexOptions.indexOptions(IndexType.Unique));
			}

			return db;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	public static InputStream limitMaxBytes(InputStream delegate, int maxBytes) {
		return new InputStream() {
			int byteCount = 0;

			private void countBytes(int readBytes) throws IOException {
				byteCount += readBytes;
				if (byteCount > maxBytes) {
					throw new IOException("Stream exceeds maximum byte limit! (" + maxBytes + ")");
				}
			}

			@Override
			public int read() throws IOException {
				int readBytes = delegate.read();
				countBytes(readBytes);
				return readBytes;
			}

			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				int readBytes = delegate.read(b, off, len);
				countBytes(readBytes);
				return readBytes;
			}
		};
	}

	public static URLConnection openConnectionWithFakeUserAgent(URL url) throws IOException {
		URLConnection hc = url.openConnection();
		hc.setRequestProperty("User-Agent",
				"Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
		return hc;
	}

	public static JSONObject readJsonFromURL(String url) throws JSONException, MalformedURLException, IOException {
		return Utils.readJsonFromStream(new URL(url).openStream());
	}

	public static String uploadToHostingService(String fileName, BufferedImage image) throws IOException {
		return uploadToHostingService(fileName, generateDiscordFriendlyPNGImage(image));
	}

	public static String uploadToHostingService(String fileName, byte[] fileData) throws IOException {
		String fileHash = generateFileHash(fileData);

		Optional<String> alreadyUploaded = checkIfUploadedAlready(fileHash);

		if (alreadyUploaded.isPresent()) {
			return alreadyUploaded.get();
		}

		Optional<BlueprintBotDiscordService> discordService = ServiceFinder
				.findService(BlueprintBotDiscordService.class);
		if (discordService.isPresent()) {
			try {
				String url = discordService.get().useDiscordForFileHosting(fileName, fileData).toString();
				addToUploadedDatabase(fileHash, url);
				return url;
			} catch (Exception e2) {
				throw new IOException("File hosting failed!", e2);
			}
		}
		throw new IOException("File hosting failed! (Discord not available)");
	}

	private WebUtils() {
	}
}
