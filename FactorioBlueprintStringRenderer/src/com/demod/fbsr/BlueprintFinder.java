package com.demod.fbsr;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipException;

import org.json.JSONObject;

import com.demod.factorio.Utils;
import com.google.common.util.concurrent.Uninterruptibles;

public final class BlueprintFinder {

	public interface Provider {
		public interface InputStreamFactory {
			public InputStream get() throws Exception;
		}

		@FunctionalInterface
		public interface Listener {
			default void handleConnection(URLConnection connection) {
				AtomicBoolean first = new AtomicBoolean(true);
				handleInputStreamFactory(() -> {
					if (first.getAndSet(false)) {
						return connection.getInputStream();
					} else {
						return WebUtils.openConnectionWithFakeUserAgent(connection.getURL()).getInputStream();
					}
				});
			}

			void handleInputStreamFactory(InputStreamFactory factory);

			default void handleURL(String url) throws Exception {
				handleConnection(WebUtils.openConnectionWithFakeUserAgent(new URL(url)));
			}

		}

		@FunctionalInterface
		public interface Mapper {
			void matched(Matcher matcher, Listener listener) throws Exception;
		}

		public Mapper getMapper();

		public Pattern getPattern();
	}

	private enum Providers implements Provider {
		PASTEBIN("pastebin\\.com/(?<id>[A-Za-z0-9]{4,})", m -> "https://pastebin.com/raw/" + m.group("id")), //

		HASTEBIN("hastebin\\.com/(?<id>[A-Za-z0-9]{4,})", m -> "https://hastebin.com/raw/" + m.group("id")), //

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

		FACTORIOPRINTS("factorioprints\\.com/view/(?<id>[-_A-Za-z0-9]+)",
				m -> "https://facorio-blueprints.firebaseio.com/blueprints/" + m.group("id") + ".json"), //

		GOOGLEDOCS("docs\\.google\\.com/document/d/(?<id>[-_A-Za-z0-9]+)",
				m -> "https://docs.google.com/document/d/" + m.group("id") + "/export?format=txt"), //
		GOOGLEDRIVE("drive\\.google\\.com/open\\?id=(?<id>[-_A-Za-z0-9]+)",
				m -> "https://drive.google.com/uc?id=" + m.group("id") + "&export=download"), //

		TEXT_URLS("\\b(?<url>(?:https?|ftp)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])", (m, l) -> {
			URL url = new URL(m.group("url"));
			URLConnection connection = WebUtils.openConnectionWithFakeUserAgent(url);
			Optional<String> contentType = Optional.ofNullable(connection.getContentType());
			if (contentType.isPresent() && contentType.get().startsWith("text/plain")) {
				l.handleConnection(connection);
			}
		}), //

		;

		private Pattern pattern;
		private final Mapper mapper;

		private Providers(String regex, Function<Matcher, String> simpleMapper) {
			this(regex, (m, l) -> l.handleURL(simpleMapper.apply(m)));
		}

		private Providers(String regex, Mapper mapper) {
			this.mapper = mapper;
			pattern = Pattern.compile(regex);
		}

		@Override
		public Mapper getMapper() {
			return mapper;
		}

		@Override
		public Pattern getPattern() {
			return pattern;
		}
	}

	private static final List<Provider> providers = new ArrayList<>();

	static {
		Arrays.stream(Providers.values()).forEach(BlueprintFinder::registerProvider);
	}

	private static final Pattern blueprintPattern = Pattern.compile("([0-9][A-Za-z0-9+\\/=\\r\\n]{90,})");

	private static void findBlueprints(InputStream in, TaskReporting reporting, Set<String> results) {
		try (Scanner scanner = new Scanner(in)) {
			String blueprintString;
			while ((blueprintString = scanner.findWithinHorizon(blueprintPattern, 4000000)) != null) {
				try {
					results.add(blueprintString);
					reporting.addBlueprintString(blueprintString);
				} catch (Exception e) {
					reporting.addException(e);
				}
			}
		}
	}

	private static void findProviders(String content, TaskReporting reporting, Set<String> results) {
		HashSet<String> uniqueCheck = new HashSet<>();
		for (Provider provider : providers) {
			Matcher matcher = provider.getPattern().matcher(content);
			while (matcher.find()) {
				try {
					String matchString = content.substring(matcher.start(), matcher.end());

					System.out.println("\t[" + provider + "] " + matchString);

					if (!uniqueCheck.add(matchString)) {
						System.out.println("\t\tDuplicate match!");
						continue;
					}

					provider.getMapper().matched(matcher, in -> {
						List<Exception> tryExceptions = new ArrayList<>();
						for (int tries = 6; tries >= 0; tries--) {
							try {
								findBlueprints(in.get(), reporting, results);
								break;
							} catch (FileNotFoundException e) {
								System.out.println("\t\tFile not Found!");
							} catch (Exception e) {
								tryExceptions.add(e);
							}
							if (tries > 1) {
								Uninterruptibles.sleepUninterruptibly(10, TimeUnit.SECONDS);
							} else {
								tryExceptions.forEach(e -> reporting.addException(e));
							}
						}
					});
				} catch (Exception e) {
					reporting.addException(e);
				}
			}
		}
	}

	public static synchronized void registerProvider(Provider provider) {
		providers.add(provider);
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
		Set<String> results = new LinkedHashSet<>();
		findBlueprints(new ByteArrayInputStream(content.getBytes()), reporting, results);
		findProviders(content, reporting, results);
		return new ArrayList<>(results);
	}

	private BlueprintFinder() {
	}
}
