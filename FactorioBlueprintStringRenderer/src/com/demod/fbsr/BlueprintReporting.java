package com.demod.fbsr;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.demod.fbsr.WorldMap.Debug;

public class BlueprintReporting {
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

	private Optional<Debug> debug = Optional.empty();
	private Optional<String> context = Optional.empty();
	private final List<String> warnings = new ArrayList<>();
	private final List<Exception> exceptions = new ArrayList<>();
	private final List<String> imageUrls = new ArrayList<>();
	private final List<String> downloadUrls = new ArrayList<>();

	public void addDownloadURL(String downloadURL) {
		downloadUrls.add(downloadURL);
	}

	public synchronized void addException(Exception e) {
		exceptions.add(e);
	}

	public void addImageURL(String imageURL) {
		imageUrls.add(imageURL);
	}

	public void addWarning(String warning) {
		warnings.add(warning);
	}

	public Optional<String> getContext() {
		return context;
	}

	public Optional<Debug> getDebug() {
		return debug;
	}

	public List<String> getDownloadURLs() {
		return downloadUrls;
	}

	public List<Exception> getExceptions() {
		return exceptions;
	}

	public List<String> getImageURLs() {
		return imageUrls;
	}

	public Level getLevel() {
		if (!warnings.isEmpty()) {
			return Level.WARN;
		}
		if (debug.isPresent()) {
			return Level.DEBUG;
		}
		if (!exceptions.isEmpty()) {
			return Level.ERROR;
		}
		return Level.INFO;
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