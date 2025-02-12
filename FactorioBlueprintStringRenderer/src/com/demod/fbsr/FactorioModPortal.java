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

import org.json.JSONArray;
import org.json.JSONObject;

public class FactorioModPortal {
	private static final String API_URL = "https://mods.factorio.com/";

	public static File downloadMod(File folder, String modName, String modVersion, String authParams)
			throws IOException {

		JSONObject jsonRelease = findModReleaseInfo(modName, modVersion);

		String downloadUrl = API_URL + jsonRelease.getString("download_url") + authParams;
		String filename = jsonRelease.getString("file_name");

		URL url = new URL(downloadUrl);
		File file = new File(folder, filename);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		try (InputStream in = conn.getInputStream(); FileOutputStream out = new FileOutputStream(file)) {
			byte[] buffer = new byte[32768];
			int bytesRead;
			while ((bytesRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, bytesRead);
			}
			System.out.println("Downloaded " + file.getAbsolutePath());
			return file;
		}
	}

	public static JSONObject findModReleaseInfo(String modName, String modVersion) throws IOException {
		URL url = new URL(API_URL + "api/mods/" + URLEncoder.encode(modName, "UTF-8"));
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
			StringBuilder responseBuilder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				responseBuilder.append(line);
			}
			JSONObject json = new JSONObject(responseBuilder.toString());
			JSONArray jsonReleases = json.getJSONArray("releases");
			for (int i = 0; i < jsonReleases.length(); i++) {
				JSONObject jsonRelease = jsonReleases.getJSONObject(i);
				if (jsonRelease.getString("version").equals(modVersion)) {
					return jsonRelease;
				}
			}
			throw new IOException("Mod not found! " + modName + " " + modVersion);
		}
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
