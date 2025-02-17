package com.demod.fbsr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class FactorioModPortal {
	private static final Logger LOGGER = LoggerFactory.getLogger(FactorioModPortal.class);
	private static final String API_URL = "https://mods.factorio.com/";

	private static Cache<String, JSONObject> cacheGet = CacheBuilder.newBuilder().maximumSize(100).softValues().build();

	private static String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	private static int compareVersions(String v1, String v2) {
		int[] n1 = Arrays.asList(v1.split("\\.")).stream().mapToInt(Integer::parseInt).toArray();
		int[] n2 = Arrays.asList(v2.split("\\.")).stream().mapToInt(Integer::parseInt).toArray();
		if (n1[0] != n2[0]) {
			return n1[0] - n2[0];
		} else if (n1[1] != n2[1]) {
			return n1[1] - n2[1];
		} else if (n1[2] != n2[2]) {
			return n1[2] - n2[2];
		}
		return 0;
	}

	public static File downloadMod(File folder, String modName, String modVersion, String authParams)
			throws IOException {

		JSONObject jsonRelease = findModReleaseInfo(modName, modVersion);

		String downloadUrl = API_URL + jsonRelease.getString("download_url") + authParams;
		String filename = jsonRelease.getString("file_name");
		String expectedSha1 = jsonRelease.getString("sha1");

		URL url = new URL(downloadUrl);
		File file = new File(folder, filename);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		try (InputStream in = conn.getInputStream(); FileOutputStream out = new FileOutputStream(file)) {
			MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
			byte[] buffer = new byte[32768];
			int bytesRead;
			while ((bytesRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, bytesRead);
				sha1Digest.update(buffer, 0, bytesRead);
			}
			LOGGER.info("Downloaded {}", file.getAbsolutePath());

			// Verify SHA-1 hash
			String fileSha1 = bytesToHex(sha1Digest.digest());
			if (!fileSha1.equalsIgnoreCase(expectedSha1)) {
				throw new IOException("SHA-1 mismatch! Expected: " + expectedSha1 + " but got: " + fileSha1);
			}
			LOGGER.info("SHA-1 hash verified successfully.");
			return file;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			System.exit(-1);
			return null;
		}
	}

	public static JSONObject findLatestModReleaseInfo(String modName) throws IOException {
		JSONObject json = get(API_URL + "api/mods/" + modName);
		JSONArray jsonReleases = json.getJSONArray("releases");
		JSONObject latestJson = jsonReleases.getJSONObject(0);
		String latestVersion = latestJson.getString("version");
		for (int i = 1; i < jsonReleases.length(); i++) {
			JSONObject jsonRelease = jsonReleases.getJSONObject(i);
			String version = jsonRelease.getString("version");
			if (compareVersions(version, latestVersion) > 0) {
				latestJson = jsonRelease;
				latestVersion = version;
			}
		}
		return latestJson;
	}

	public static JSONObject findLatestModReleaseInfoFull(String modName) throws IOException {
		JSONObject json = get(API_URL + "api/mods/" + modName + "/full");
		JSONArray jsonReleases = json.getJSONArray("releases");
		JSONObject latestJson = jsonReleases.getJSONObject(0);
		String latestVersion = latestJson.getString("version");
		for (int i = 1; i < jsonReleases.length(); i++) {
			JSONObject jsonRelease = jsonReleases.getJSONObject(i);
			String version = jsonRelease.getString("version");
			if (compareVersions(version, latestVersion) > 0) {
				latestJson = jsonRelease;
				latestVersion = version;
			}
		}
		return latestJson;
	}

	public static String findLatestModVersion(String modName) throws IOException {
		JSONObject json = get(API_URL + "api/mods/" + modName);
		JSONArray jsonReleases = json.getJSONArray("releases");
		String latestVersion = jsonReleases.getJSONObject(0).getString("version");
		for (int i = 1; i < jsonReleases.length(); i++) {
			String version = jsonReleases.getJSONObject(i).getString("version");
			if (compareVersions(version, latestVersion) > 0) {
				latestVersion = version;
			}
		}
		return latestVersion;
	}

	public static JSONObject findModReleaseInfo(String modName, String modVersion) throws IOException {
		JSONObject json = get(API_URL + "api/mods/" + modName);
		JSONArray jsonReleases = json.getJSONArray("releases");
		for (int i = 0; i < jsonReleases.length(); i++) {
			JSONObject jsonRelease = jsonReleases.getJSONObject(i);
			if (jsonRelease.getString("version").equals(modVersion)) {
				return jsonRelease;
			}
		}
		throw new IOException("Mod not found! " + modName + " " + modVersion);
	}

	public static JSONObject findModReleaseInfoFull(String modName, String modVersion) throws IOException {
		JSONObject json = get(API_URL + "api/mods/" + modName + "/full");
		JSONArray jsonReleases = json.getJSONArray("releases");
		for (int i = 0; i < jsonReleases.length(); i++) {
			JSONObject jsonRelease = jsonReleases.getJSONObject(i);
			if (jsonRelease.getString("version").equals(modVersion)) {
				return jsonRelease;
			}
		}
		throw new IOException("Mod not found! " + modName + " " + modVersion);
	}

	private static JSONObject get(String url) throws IOException {
		JSONObject json = cacheGet.getIfPresent(url);
		if (json == null) {
			@SuppressWarnings("deprecation")
			HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
				StringBuilder responseBuilder = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) {
					responseBuilder.append(line);
				}
				json = new JSONObject(responseBuilder.toString());
				cacheGet.put(url, json);
			}
		}
		return json;
	}

	public static String getAuthParams(String username, String password) throws IOException {
		URL url = new URL("https://auth.factorio.com/api-login");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		conn.setDoOutput(true);

		String postData = "username=" + URLEncoder.encode(username, "UTF-8") + "&password="
				+ URLEncoder.encode(password, "UTF-8") + "&api_version=6";
		try (OutputStream os = conn.getOutputStream()) {
			os.write(postData.getBytes(StandardCharsets.UTF_8));
		}

		if (conn.getResponseCode() == 200) {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
				StringBuilder responseBuilder = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) {
					responseBuilder.append(line);
				}
				JSONObject json = new JSONObject(responseBuilder.toString());
				if (json.has("error")) {
					throw new IOException(
							"Authentication failed: (" + json.getString("error") + ") " + json.optString("message"));
				}
				return "?username=" + json.getString("username") + "&token=" + json.getString("token");
			}
		} else {
			throw new IOException("Authentication failed: " + conn.getResponseMessage());
		}
	}
}
