package com.demod.fbsr;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.demod.factorio.Utils;
import com.demod.fbsr.app.BlueprintBotDiscordService;
import com.demod.fbsr.app.ServiceFinder;
import com.google.common.util.concurrent.Uninterruptibles;

import javafx.util.Pair;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;

public final class WebUtils {
	public static void addPossiblyLargeEmbedField(EmbedBuilder builder, String name, String value, boolean inline)
			throws IOException {
		if (value.length() <= MessageEmbed.VALUE_MAX_LENGTH) {
			builder.addField(name, value, inline);
		} else {
			builder.addField(name + " Link", uploadToHostingService(name + ".txt", value.getBytes()).toString(),
					inline);
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

	public static JSONObject readJsonFromURL(String url) throws JSONException, MalformedURLException, IOException {
		return Utils.readJsonFromStream(new URL(url).openStream());
	}

	public static URL uploadToBundly(String title, String description, List<Pair<URL, String>> links)
			throws IOException {
		JSONObject request = new JSONObject();
		request.put("title", title);
		request.put("desc", description);

		JSONArray items = new JSONArray();
		for (Pair<URL, String> pair : links) {
			JSONObject item = new JSONObject();
			item.put("url", pair.getKey().toString());
			item.put("id", Long.toString(System.currentTimeMillis()));
			item.put("caption", pair.getValue());
			items.put(item);

			// XXX Lazy approach to make sure id is unique...
			Uninterruptibles.sleepUninterruptibly(1, TimeUnit.MILLISECONDS);
		}
		request.put("items", items);

		URL url = new URL("http://bundly.io/createBundle");
		URLConnection connection = url.openConnection();
		connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
		connection.setDoOutput(true);
		connection.getOutputStream().write(request.toString().getBytes(StandardCharsets.UTF_8));
		connection.getOutputStream().flush();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
			String line = br.readLine();
			if (line == null || line.length() > 8) {
				throw new IOException("Bundly.io returned an invalid response: " + line);
			}
			return new URL("http://bundly.io/" + line);
		}
	}

	public static URL uploadToHostingService(String fileName, BufferedImage image) throws IOException {
		Optional<BlueprintBotDiscordService> discordService = ServiceFinder
				.findService(BlueprintBotDiscordService.class);

		if (discordService.isPresent()) {
			// Discord original -> nya.is -> Discord scaled
			try {
				return discordService.get().useDiscordForImageHosting(fileName, image, false);
			} catch (Exception e) {
				try {
					return uploadToMixtapeMoe(fileName, getImageData(image));
				} catch (Exception e1) {
					return discordService.get().useDiscordForImageHosting(fileName, image, true);
				}
			}
		} else {
			return uploadToMixtapeMoe(fileName, getImageData(image));
		}
	}

	public static URL uploadToHostingService(String fileName, byte[] fileData) throws IOException {
		try {
			return uploadToMixtapeMoe(fileName, fileData);
		} catch (Exception e) {
			Optional<BlueprintBotDiscordService> discordService = ServiceFinder
					.findService(BlueprintBotDiscordService.class);
			if (discordService.isPresent()) {
				try {
					return discordService.get().useDiscordForFileHosting(fileName, fileData);
				} catch (Exception e2) {
				}
			}
			throw new IOException("File hosting failed!");
		}
	}

	private static URL uploadToMixtapeMoe(String fileName, byte[] fileData) throws IOException {
		try (ByteArrayInputStream bais = new ByteArrayInputStream(fileData)) {
			// XXX You can get a copy of the MultipartUtility from
			// http://www.codejava.net/java-se/networking/upload-files-by-sending-multipart-request-programmatically
			MultipartUtility utility = new MultipartUtility("https://mixtape.moe/upload.php", "UTF-8");
			utility.addFormField("name", fileName);
			utility.addFilePart("files[]", fileName, bais);
			return new URL(
					new JSONObject(utility.finish().get(0)).getJSONArray("files").getJSONObject(0).getString("url"));
		}
	}

	@SuppressWarnings("unused")
	private static URL uploadToNyaIs(String fileName, byte[] fileData) throws IOException {
		try (ByteArrayInputStream bais = new ByteArrayInputStream(fileData)) {
			// XXX You can get a copy of the MultipartUtility from
			// http://www.codejava.net/java-se/networking/upload-files-by-sending-multipart-request-programmatically
			MultipartUtility utility = new MultipartUtility("https://nya.is/upload", "UTF-8");
			utility.addFormField("name", fileName);
			utility.addFilePart("files[]", fileName, bais);
			return new URL(
					new JSONObject(utility.finish().get(0)).getJSONArray("files").getJSONObject(0).getString("url"));
		}
	}

	private WebUtils() {
	}
}
