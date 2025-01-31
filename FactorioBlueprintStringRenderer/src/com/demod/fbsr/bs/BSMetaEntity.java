package com.demod.fbsr.bs;

import java.util.Optional;

import org.json.JSONObject;

import com.demod.fbsr.legacy.LegacyBlueprintEntity;

public class BSMetaEntity extends BSEntity {

	private final JSONObject json;
	private final LegacyBlueprintEntity legacy;
	private Optional<Exception> parseException = Optional.empty();

	public BSMetaEntity(JSONObject json) {
		super(json);

		this.json = json;
		this.legacy = null;
	}

	public BSMetaEntity(LegacyBlueprintEntity legacy) {
		super(legacy);

		this.json = null;
		this.legacy = legacy;
	}

	public JSONObject getJson() {
		return json;
	}

	public LegacyBlueprintEntity getLegacy() {
		return legacy;
	}

	public Optional<Exception> getParseException() {
		return parseException;
	}

	public boolean isLegacy() {
		return legacy != null;
	}

	public void setParseException(Optional<Exception> parseException) {
		this.parseException = parseException;
	}
}
