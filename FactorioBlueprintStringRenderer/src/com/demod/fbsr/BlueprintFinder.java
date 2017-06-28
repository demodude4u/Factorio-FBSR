package com.demod.fbsr;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipException;

import org.json.JSONObject;

import com.demod.factorio.Utils;
import com.google.common.util.concurrent.Uninterruptibles;

public final class BlueprintFinder {

	public enum Provider {

		PASTEBIN("pastebin\\.com/(?<id>[A-Za-z0-9]+)", m -> "https://pastebin.com/raw/" + m.group("id")), //

		HASTEBIN("hastebin\\.com/(?<id>[A-Za-z0-9]+)", m -> "https://hastebin.com/raw/" + m.group("id")), //

		GITLAB("gitlab\\.com/snippets/(?<id>[A-Za-z0-9]+)",
				m -> "https://gitlab.com/snippets/" + m.group("id") + "/raw"), //

		GIST("gist\\.github\\.com/[-a-zA-Z0-9]+/(?<id>[a-z0-9]+)", (m, l) -> {
			JSONObject response = WebUtils.readJsonFromURL("https://api.github.com/gists/" + m.group("id"));
			JSONObject filesJson = response.getJSONObject("files");
			Utils.<JSONObject>forEach(filesJson, (k, v) -> {
				if (v.getString("type").startsWith("text/plain")) {
					l.handleURL(v.getString("raw_url"));
				}
			});
		}), //

		DROPBOX("\\b(?<url>https://www\\.dropbox\\.com/s/[^\\s?]+)", m -> m.group("url") + "?raw=1"), //

		TEXT_URLS("\\b(?<url>(?:https?|ftp)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])", (m, l) -> {
			URL url = new URL(m.group("url"));
			URLConnection connection = WebUtils.openConnectionWithFakeUserAgent(url);
			if (connection.getContentType().startsWith("text/plain")) {
				l.handleConnection(connection);
			}
		}), //

		;
		@FunctionalInterface
		private interface Listener {
			void handleConnection(URLConnection connection);

			default void handleURL(String url) throws Exception {
				handleConnection(WebUtils.openConnectionWithFakeUserAgent(new URL(url)));
			}
		}

		@FunctionalInterface
		private interface Mapper {
			void matched(Matcher matcher, Listener listener) throws Exception;
		}

		private Pattern pattern;
		private final Mapper mapper;

		private Provider(String regex, Function<Matcher, String> simpleMapper) {
			this(regex, (m, l) -> l.handleURL(simpleMapper.apply(m)));
		}

		private Provider(String regex, Mapper mapper) {
			this.mapper = mapper;
			pattern = Pattern.compile(regex);
		}
	}

	private static final Pattern blueprintPattern = Pattern.compile("([0-9][A-Za-z0-9+\\/=\\r\\n]{90,})");

	private static void findBlueprints(InputStream in, TaskReporting reporting, List<String> results) {
		try (Scanner scanner = new Scanner(in)) {
			String blueprintString;
			while ((blueprintString = scanner.findWithinHorizon(blueprintPattern, 4000000)) != null) {
				try {
					results.add(blueprintString);
				} catch (Exception e) {
					reporting.addException(e);
				}
			}
		}
	}

	private static void findProviders(String content, TaskReporting reporting, List<String> results) {
		for (Provider provider : Provider.values()) {
			Matcher matcher = provider.pattern.matcher(content);
			while (matcher.find()) {
				try {
					provider.mapper.matched(matcher, in -> {
						try {
							List<IOException> tryExceptions = new ArrayList<>();
							for (int tries = 6; tries >= 0; tries--) {
								try {
									findBlueprints(in.getInputStream(), reporting, results);
									break;
								} catch (IOException e) {
									tryExceptions.add(e);
								}
								if (tries > 1) {
									Uninterruptibles.sleepUninterruptibly(10, TimeUnit.SECONDS);
									in = WebUtils.openConnectionWithFakeUserAgent(in.getURL());
								} else {
									tryExceptions.forEach(e -> reporting.addException(e));
								}
							}
						} catch (IOException e) {
							reporting.addException(e);
						}
					});
				} catch (Exception e) {
					reporting.addException(e);
				}
			}
		}
	}

	public static List<BlueprintStringData> search(String content, TaskReporting reporting) {
		List<BlueprintStringData> results = new ArrayList<>();
		for (String blueprintString : searchRaw(content, reporting)) {
			try {
				results.add(new BlueprintStringData(blueprintString));
			} catch (ZipException e) {
				reporting.addInfo("Sorry, but I can't read those kind of blueprints just yet.");
			} catch (IllegalArgumentException | IOException e) {
				reporting.addException(e);
			}
		}
		return results;
	}

	public static List<String> searchRaw(String content, TaskReporting reporting) {
		List<String> results = new ArrayList<>();
		findBlueprints(new ByteArrayInputStream(content.getBytes()), reporting, results);
		findProviders(content, reporting, results);
		return results;
	}

	private BlueprintFinder() {
	}
}
