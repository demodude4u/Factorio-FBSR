package com.demod.fbsr;

import java.util.ArrayList;
import java.util.List;

public class BlueprintReporting {
	private final List<String> contexts = new ArrayList<>();
	private final List<String> warnings = new ArrayList<>();
	private final List<Exception> exceptions = new ArrayList<>();
	private final List<String> imageUrls = new ArrayList<>();
	private final List<String> downloadUrls = new ArrayList<>();

	public synchronized void addContext(String context) {
		contexts.add(context);
	}

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

	public List<String> getContexts() {
		return contexts;
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

	public List<String> getWarnings() {
		return warnings;
	}
}