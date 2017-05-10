package com.demod.fbsr;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.InflaterInputStream;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONObject;

public class BlueprintStringData {
	private final List<Blueprint> blueprints = new ArrayList<>();
	private final int version;

	public BlueprintStringData(String blueprintString) throws IllegalArgumentException, IOException {
		version = Integer.parseInt(blueprintString.substring(0, 1));
		byte[] decoded = Base64.decodeBase64(blueprintString.substring(1));
		JSONObject json;
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(new InflaterInputStream(new ByteArrayInputStream(decoded))))) {
			StringBuilder jsonBuilder = new StringBuilder();
			String line;
			while ((line = br.readLine()) != null) {
				jsonBuilder.append(line);
			}
			json = new JSONObject(jsonBuilder.toString());
		}
		System.out.println(json);
		if (json.has("blueprint")) {
			blueprints.add(new Blueprint(json));
		} else {
			JSONArray blueprintsJson = json.getJSONObject("blueprint_book").getJSONArray("blueprints");
			for (int i = 0; i < blueprintsJson.length(); i++) {
				blueprints.add(new Blueprint(blueprintsJson.getJSONObject(i)));
			}
		}
	}

	public List<Blueprint> getBlueprints() {
		return blueprints;
	}

	public int getVersion() {
		return version;
	}
}
