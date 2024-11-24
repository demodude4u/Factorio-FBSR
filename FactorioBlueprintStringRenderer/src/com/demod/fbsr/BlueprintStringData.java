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
import org.json.JSONObject;

import com.demod.fbsr.bs.BSBlueprint;
import com.demod.fbsr.bs.BSBlueprintString;
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

	private final List<BSBlueprint> blueprints = new ArrayList<>();

	private final JSONObject json;

	private Optional<String> label = null;

	private final String blueprintStringRaw;

	public BlueprintStringData(String string) throws IllegalArgumentException, IOException {
		this.blueprintStringRaw = string;
		String versionChar = string.substring(0, 1);
		try {
			if (Integer.parseInt(versionChar) != 0) {
				throw new IllegalArgumentException("Only Version 0 is supported! (" + versionChar + ")");
			}
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Version is not valid! (" + versionChar + ")");
		}

		json = decode(string);
		BSBlueprintString blueprintString = new BSBlueprintString(json);

		findBlueprints(blueprintString);

		if (blueprints.isEmpty()) {
			throw new IllegalArgumentException("No blueprints found in blueprint string!");
		}
	}

	private void findBlueprints(BSBlueprintString blueprintString) {
		blueprintString.blueprint.ifPresent(bs -> {
			blueprints.add(bs);
			if (label == null) {
				label = bs.label;
			}
		});
		blueprintString.blueprintBook.ifPresent(bb -> {
			bb.blueprints.forEach(this::findBlueprints);
			if (label == null) {
				label = bb.label;
			}
		});
	}

	public List<BSBlueprint> getBlueprints() {
		return blueprints;
	}

	public Optional<String> getLabel() {
		return label;
	}

	public boolean isBook() {
		return blueprints.size() > 1;
	}

	public JSONObject json() {
		return json;
	}

	@Override
	public String toString() {
		return blueprintStringRaw;
	}
}
