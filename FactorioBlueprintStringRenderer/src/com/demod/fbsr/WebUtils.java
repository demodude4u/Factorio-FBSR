package com.demod.fbsr;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.json.JSONException;
import org.json.JSONObject;

import com.demod.factorio.Utils;

import net.dv8tion.jda.api.EmbedBuilder;

public final class WebUtils {

	public static void addPossiblyLargeEmbedField(EmbedBuilder builder, String name, String value, boolean inline)
			throws IOException {
		// TODO need a new way to handle this
//		if (value.length() <= MessageEmbed.VALUE_MAX_LENGTH) {
		builder.addField(name, value, inline);
//		} else {
//			builder.addField(name + " Link", uploadToHostingService(name + ".txt", value.getBytes()).toString(),
//					inline);
//		}
	}

	public static String formatBlueprintFilename(Optional<String> label, String extension) {
		if (label.isPresent()) {
			String filename = "blueprint-"
					+ label.get().replaceAll("[^a-zA-Z0-9\\-\\s]", "").replaceAll("\\s+", "-").toLowerCase();
			if (filename.length() > 50) {
				filename = filename.substring(0, 50);
			}
			filename += "." + extension;
			return filename;
		} else {
			return "blueprint." + extension;
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

	// TODO need a new hosting system that does not deliver expired URLs

//	public static String uploadToHostingService(String fileName, BufferedImage image) throws IOException {
//		return uploadToHostingService(fileName, generateDiscordFriendlyPNGImage(image));
//	}
//
//	public static String uploadToHostingService(String fileName, byte[] fileData) throws IOException {
//		Optional<BlueprintBotDiscordService> discordService = ServiceFinder
//				.findService(BlueprintBotDiscordService.class);
//		if (discordService.isPresent()) {
//			try {
//				String url = discordService.get().useDiscordForFileHosting(fileName, fileData).get().getAttachments().get(0).getUrl().toString();
//				return url;
//			} catch (Exception e2) {
//				throw new IOException("File hosting failed!", e2);
//			}
//		}
//		throw new IOException("File hosting failed! (Discord not available)");
//	}

	public static JSONObject readJsonFromURL(String url) throws JSONException, MalformedURLException, IOException {
		return Utils.readJsonFromStream(new URL(url).openStream());
	}

	private WebUtils() {
	}
}
