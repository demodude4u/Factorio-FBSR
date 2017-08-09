package com.demod.fbsr;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.base.Charsets;

public class BlueprintStringData {
	private static String cleanupBlueprintString(String blueprintString) {
		// Remove new lines
		blueprintString = blueprintString.replaceAll("\\r|\\n", "");

		return blueprintString;
	}

	public static JSONObject decode(String blueprintString) throws IOException {
		blueprintString = cleanupBlueprintString(blueprintString);
		byte[] decoded = Base64.decodeBase64(blueprintString.substring(1));
		JSONObject json;
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(new InflaterInputStream(new ByteArrayInputStream(decoded)), Charsets.UTF_8))) {
			StringBuilder jsonBuilder = new StringBuilder();
			String line;
			while ((line = br.readLine()) != null) {
				jsonBuilder.append(line);
			}
			json = new JSONObject(jsonBuilder.toString());
		}
		return json;
	}

	public static String encode(JSONObject json) throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DeflaterOutputStream dos = new DeflaterOutputStream(baos)) {
			dos.write(json.toString().getBytes());
			dos.close();
			return "0" + Base64.encodeBase64String(baos.toByteArray());
		}
	}

	private final List<Blueprint> blueprints = new ArrayList<>();

	private final JSONObject json;

	private final Optional<String> label;
	private final Optional<Long> version;

	public BlueprintStringData(String blueprintString) throws IllegalArgumentException, IOException {
		String versionChar = blueprintString.substring(0, 1);
		try {
			if (Integer.parseInt(versionChar) != 0) {
				throw new IllegalArgumentException("Only Version 0 is supported! (" + versionChar + ")");
			}
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Version is not valid! (" + versionChar + ")");
		}

		json = decode(blueprintString);

		if (json.has("blueprint")) {
			Blueprint blueprint = new Blueprint(json);
			blueprints.add(blueprint);

			label = blueprint.getLabel();
			version = blueprint.getVersion();
		} else {
			JSONObject bookJson = json.getJSONObject("blueprint_book");
			JSONArray blueprintsJson = bookJson.getJSONArray("blueprints");
			for (int i = 0; i < blueprintsJson.length(); i++) {
				Blueprint blueprint = new Blueprint(blueprintsJson.getJSONObject(i));
				blueprints.add(blueprint);
			}

			if (bookJson.has("label")) {
				label = Optional.of(bookJson.getString("label"));
			} else {
				label = Optional.empty();
			}
			if (bookJson.has("version")) {
				version = Optional.of(bookJson.getLong("version"));
			} else {
				version = Optional.empty();
			}
		}

		if (blueprints.isEmpty()) {
			throw new IllegalArgumentException("No blueprints found in blueprint string!");
		}
	}

	public List<Blueprint> getBlueprints() {
		return blueprints;
	}

	public Optional<String> getLabel() {
		return label;
	}

	public Optional<Long> getVersion() {
		return version;
	}

	public boolean isBook() {
		return blueprints.size() > 1;
	}

	public JSONObject json() {
		return json;
	}
}
