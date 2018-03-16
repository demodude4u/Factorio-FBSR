package com.demod.fbsr;

import java.awt.Color;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import com.demod.fbsr.WorldMap.Debug;

public class TaskReporting {
	public static enum Level {
		INFO(Color.gray), WARN(Color.orange), ERROR(Color.red), DEBUG(Color.magenta);
		private final Color color;

		private Level(Color color) {
			this.color = color;
		}

		public Color getColor() {
			return color;
		}
	}

	private final List<String> blueprintStrings = new ArrayList<>();
	private Optional<Debug> debug = Optional.empty();
	private Optional<String> context = Optional.empty();
	private final List<String> warnings = new ArrayList<>();
	private final List<Exception> exceptions = new ArrayList<>();
	private final List<Entry<Optional<String>, String>> images = new ArrayList<>();
	private final List<String> downloads = new ArrayList<>();
	private final List<String> links = new ArrayList<>();
	private final Set<String> info = new LinkedHashSet<>();

	public void addBlueprintString(String blueprintString) {
		blueprintStrings.add(blueprintString);
	}

	public void addDownload(String url) {
		downloads.add(url);
	}

	public synchronized void addException(Exception e) {
		exceptions.add(e);
	}

	public void addImage(Optional<String> label, String url) {
		images.add(new SimpleEntry<>(label, url));
	}

	public void addInfo(String info) {
		this.info.add(info);
	}

	public void addLink(String url) {
		links.add(url);
	}

	public void addWarning(String warning) {
		warnings.add(warning);
	}

	public List<String> getBlueprintStrings() {
		return blueprintStrings;
	}

	public Optional<String> getContext() {
		return context;
	}

	public Optional<Debug> getDebug() {
		return debug;
	}

	public List<String> getDownloads() {
		return downloads;
	}

	public List<Exception> getExceptions() {
		return exceptions;
	}

	public List<Entry<Optional<String>, String>> getImages() {
		return images;
	}

	public Set<String> getInfo() {
		return info;
	}

	public Level getLevel() {
		if (debug.isPresent()) {
			return Level.DEBUG;
		}
		if (!exceptions.isEmpty()) {
			return Level.ERROR;
		}
		if (!warnings.isEmpty()) {
			return Level.WARN;
		}
		return Level.INFO;
	}

	public List<String> getLinks() {
		return links;
	}

	public List<String> getWarnings() {
		return warnings;
	}

	public synchronized void setContext(String context) {
		this.context = Optional.of(context);
	}

	public void setDebug(Optional<Debug> debug) {
		this.debug = debug;
	}

}