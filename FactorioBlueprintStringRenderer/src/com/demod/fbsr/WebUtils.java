package com.demod.fbsr;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
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
import com.demod.fbsr.app.BlueprintBotDiscordService;
import com.demod.fbsr.app.ServiceFinder;

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

	public static URL uploadToHostingService(String fileName, BufferedImage image) throws IOException {
		try (ByteArrayInputStream bais = new ByteArrayInputStream(getImageData(image))) {
			return uploadToNyaIs(fileName, bais);
		} catch (Exception e) {
			Optional<BlueprintBotDiscordService> discordService = ServiceFinder
					.findService(BlueprintBotDiscordService.class);
			if (discordService.isPresent()) {
				try {
					return discordService.get().useDiscordForImageHosting(fileName, image);
				} catch (Exception e2) {
				}
			}
			throw new IOException("Image hosting failed!");
		}
	}

	public static URL uploadToHostingService(String fileName, byte[] fileData) throws IOException {
		try (ByteArrayInputStream bais = new ByteArrayInputStream(fileData)) {
			return uploadToNyaIs(fileName, bais);
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

	private static URL uploadToNyaIs(String fileName, InputStream inputStream) throws IOException {
		// XXX You can get a copy of the MultipartUtility from
		// http://www.codejava.net/java-se/networking/upload-files-by-sending-multipart-request-programmatically
		MultipartUtility utility = new MultipartUtility("https://nya.is/upload", "UTF-8");
		utility.addFormField("name", fileName);
		utility.addFilePart("files[]", fileName, inputStream);
		return new URL(new JSONObject(utility.finish().get(0)).getJSONArray("files").getJSONObject(0).getString("url"));
	}

	private WebUtils() {
	}
}
