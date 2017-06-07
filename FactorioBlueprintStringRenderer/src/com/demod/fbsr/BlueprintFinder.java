package com.demod.fbsr;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

import com.demod.factorio.Utils;

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
				if (v.getString("type").equals("text/plain")) {
					l.addURL(v.getString("raw_url"));
				}
			});
		}), //

		TEXT_URLS("\\b(?<url>(?:https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])", (m, l) -> {
			URL url = new URL(m.group("url"));
			URLConnection connection = url.openConnection();
			if (connection.getContentType().startsWith("text/plain")) {
				l.addURL(m.group("url"));
			}
		}), //

		;
		@FunctionalInterface
		private interface Listener {
			void addURL(String url) throws Exception;
		}

		@FunctionalInterface
		private interface Mapper {
			void matched(Matcher matcher, Listener listener) throws Exception;
		}

		private Pattern pattern;
		private final Mapper mapper;

		private Provider(String regex, Function<Matcher, String> simpleMapper) {
			this(regex, (m, l) -> l.addURL(simpleMapper.apply(m)));
		}

		private Provider(String regex, Mapper mapper) {
			this.mapper = mapper;
			pattern = Pattern.compile(regex);
		}
	}

	private static final Pattern blueprintPattern = Pattern.compile("([0-9][A-Za-z0-9+\\/=\\r\\n]{90,})");

	private static void findBlueprints(InputStream in, BlueprintReporting reporting,
			List<BlueprintStringData> results) {
		try (Scanner scanner = new Scanner(in)) {
			String blueprintString;
			while ((blueprintString = scanner.findWithinHorizon(blueprintPattern, 4000000)) != null) {
				try {
					reporting.addContext(blueprintString);
					results.add(new BlueprintStringData(blueprintString));
				} catch (Exception e) {
					reporting.addException(e);
				}
			}
		}
	}

	private static void findProviders(String content, BlueprintReporting reporting, List<BlueprintStringData> results) {
		int initialResultCount = results.size();
		for (Provider provider : Provider.values()) {
			Matcher matcher = provider.pattern.matcher(content);
			if (matcher.find()) {
				try {
					provider.mapper.matched(matcher, in -> {
						findBlueprints(new URL(in).openStream(), reporting, results);
					});
				} catch (Exception e) {
					reporting.addException(e);
				}
			}
			if (results.size() != initialResultCount) {
				reporting.addContext(content);
				break;
			}
		}
	}

	public static List<BlueprintStringData> search(String content, BlueprintReporting reporting) {
		List<BlueprintStringData> results = new ArrayList<>();
		findBlueprints(new ByteArrayInputStream(content.getBytes()), reporting, results);
		findProviders(content, reporting, results);
		return results;
	}

	private BlueprintFinder() {
	}
}
