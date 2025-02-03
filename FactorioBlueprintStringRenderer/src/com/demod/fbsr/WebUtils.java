package com.demod.fbsr;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.json.JSONException;
import org.json.JSONObject;

import com.demod.factorio.Config;
import com.demod.factorio.Utils;

import net.dv8tion.jda.api.EmbedBuilder;

public final class WebUtils {

	private static Optional<String> IMGBB_API_KEY = Optional.empty();

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

	private static synchronized String getImgBBAPIKey() {
		if (IMGBB_API_KEY.isPresent()) {
			return IMGBB_API_KEY.get();
		}

		JSONObject configJson = Config.get().getJSONObject("reddit");
		if (!configJson.has("imgbb-api-key")) {
			throw new IllegalStateException("Missing ImgBB API Key!");
		}
		IMGBB_API_KEY = Optional.of(configJson.getString("imgbb-api-key"));
		return IMGBB_API_KEY.get();
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

	public static String uploadToImgBB(BufferedImage image, String name) throws IOException {
		// 1. Convert BufferedImage to base64
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(image, "png", baos);
		String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());

		String endpoint = "https://api.imgbb.com/1/upload?key=" + getImgBBAPIKey();
		String postData = "image=" + URLEncoder.encode(base64Image, StandardCharsets.UTF_8) + "&name="
				+ URLEncoder.encode(name, StandardCharsets.UTF_8);

		URL url = new URL(endpoint);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		conn.setDoOutput(true);

		try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
			dos.writeBytes(postData);
			dos.flush();
		}

		int responseCode = conn.getResponseCode();
		if (responseCode != HttpURLConnection.HTTP_OK) {
			throw new IOException("ImgBB upload failed with HTTP code: " + responseCode);
		}

		StringBuilder responseBuilder = new StringBuilder();
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = br.readLine()) != null) {
				responseBuilder.append(line);
			}
		}
		conn.disconnect();

		JSONObject json = new JSONObject(responseBuilder.toString());
		if (!json.isNull("data")) {
			JSONObject dataObject = json.getJSONObject("data");
			if (!dataObject.isNull("url")) {
				return dataObject.getString("url");
			}
		}

		throw new IOException("Failed to parse ImgBB response. Data or display_url is missing.");
	}

	private WebUtils() {
	}
}
