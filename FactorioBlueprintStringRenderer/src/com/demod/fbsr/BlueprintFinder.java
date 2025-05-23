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
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.factorio.Utils;
import com.demod.fbsr.bs.BSBlueprintString;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Uninterruptibles;

public final class BlueprintFinder {

	private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintFinder.class);

	public static class FindBlueprintRawResult {
		public final Optional<String> encodedData;
		public final Optional<JSONObject> decodedData;
		public final Optional<Exception> failureCause;

		public FindBlueprintRawResult(Optional<String> encodedData, Optional<JSONObject> decodedData,
				Optional<Exception> failureCause) {
			this.encodedData = encodedData;
			this.decodedData = decodedData;
			this.failureCause = failureCause;
		}
	}

	public static class FindBlueprintResult {
		public final Optional<BSBlueprintString> blueprintString;
		public Optional<Exception> failureCause;

		public FindBlueprintResult(Optional<BSBlueprintString> blueprintString, Optional<Exception> failureCause) {
			this.blueprintString = blueprintString;
			this.failureCause = failureCause;
		}
	}

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
				if (CONTENT_TYPES.stream().anyMatch(s -> v.getString("type").startsWith(s))) {
					l.handleURL(v.getString("raw_url"));
				}
			});
		}), //

		DROPBOX("\\b(?<url>https://www\\.dropbox\\.com/s/[^\\s?]+)", m -> m.group("url") + "?raw=1"), //

		FACTORIOPRINTS("factorioprints\\.com/view/(?<id>[-_A-Za-z0-9]+)",
				m -> "https://facorio-blueprints.firebaseio.com/blueprints/" + m.group("id") + ".json"), //
		FACTORIOSCHOOL("factorio\\.school/view/(?<id>[-_A-Za-z0-9]+)",
				m -> "https://facorio-blueprints.firebaseio.com/blueprints/" + m.group("id") + ".json"), //

		GOOGLEDOCS("docs\\.google\\.com/document/d/(?<id>[-_A-Za-z0-9]+)",
				m -> "https://docs.google.com/document/d/" + m.group("id") + "/export?format=txt"), //
		GOOGLEDRIVE("drive\\.google\\.com/open\\?id=(?<id>[-_A-Za-z0-9]+)",
				m -> "https://drive.google.com/uc?id=" + m.group("id") + "&export=download"), //

		TEXT_URLS("\\b(?<url>(?:https?|ftp)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])", (m, l) -> {
			URL url = new URL(m.group("url"));
			URLConnection connection = WebUtils.openConnectionWithFakeUserAgent(url);
			Optional<String> contentType = Optional.ofNullable(connection.getContentType());
			if (contentType.isPresent() && CONTENT_TYPES.stream().anyMatch(s -> contentType.get().startsWith(s))) {
				l.handleConnection(connection);
			}
		}), //

		;

		private final Pattern pattern;
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

	public static List<String> CONTENT_TYPES = ImmutableList.of("text/plain", "application/json");

	private static final List<Provider> providers = new ArrayList<>();

	static {
		Arrays.stream(Providers.values()).forEach(BlueprintFinder::registerProvider);
	}

	private static final Pattern blueprintPattern = Pattern.compile("([0-9][A-Za-z0-9+\\/=\\r\\n]{90,})");

	private static void findBlueprints(InputStream in, List<FindBlueprintRawResult> results) {
		String content;
		try {
			content = new String(in.readNBytes(20000000));
		} catch (IOException e) {
			results.add(new FindBlueprintRawResult(Optional.empty(), Optional.empty(), Optional.of(e)));
			return;
		}
		boolean hasEncoded = false;
		try (Scanner scanner = new Scanner(content)) {
			String blueprintString;
			while ((blueprintString = scanner.findWithinHorizon(blueprintPattern, 0)) != null) {
				results.add(
						new FindBlueprintRawResult(Optional.of(blueprintString), Optional.empty(), Optional.empty()));
				hasEncoded = true;
			}
		}
		if (!hasEncoded) {// Check for decoded
			try {
				int first = content.indexOf('{');
				int last = content.lastIndexOf('}');
				if (first != -1 && last != -1) {
					JSONObject json = new JSONObject(content.substring(first, last + 1));
					if (json.has("blueprint") //
							|| json.has("blueprint_book") //
							|| json.has("upgrade_planner") //
							|| json.has("deconstruction_planner")) {
						results.add(new FindBlueprintRawResult(Optional.empty(), Optional.of(json), Optional.empty()));
					}
				}
			} catch (JSONException e) {
				results.add(new FindBlueprintRawResult(Optional.empty(), Optional.empty(), Optional.of(e)));
			}
		}
	}

	private static void findProviders(String content, List<FindBlueprintRawResult> results) {
		HashSet<String> uniqueCheck = new HashSet<>();
		for (Provider provider : providers) {
			Matcher matcher = provider.getPattern().matcher(content);
			while (matcher.find()) {
				try {
					String matchString = content.substring(matcher.start(), matcher.end());

					LOGGER.info("\t[{}] {}", provider, matchString);

					if (!uniqueCheck.add(matchString)) {
						LOGGER.info("\t\tDuplicate match!");
						continue;
					}

					provider.getMapper().matched(matcher, in -> {
						List<Exception> tryExceptions = new ArrayList<>();
						for (int tries = 6; tries >= 0; tries--) {
							try {
								findBlueprints(in.get(), results);
								break;
							} catch (FileNotFoundException e) {
								LOGGER.info("\t\tFile not Found!");
							} catch (Exception e) {
								tryExceptions.add(e);
							}
							if (tries > 1) {
								Uninterruptibles.sleepUninterruptibly(10, TimeUnit.SECONDS);
							} else {
								tryExceptions.forEach(e -> results.add(new FindBlueprintRawResult(Optional.empty(),
										Optional.empty(), Optional.of(e))));
							}
						}
					});
				} catch (Exception e) {
					results.add(new FindBlueprintRawResult(Optional.empty(), Optional.empty(), Optional.of(e)));
				}
			}
		}
	}

	public static synchronized void registerProvider(Provider provider) {
		providers.add(provider);
	}

	public static List<FindBlueprintResult> search(String content) {
		List<FindBlueprintResult> results = new ArrayList<>();
		for (FindBlueprintRawResult result : searchRaw(content)) {
			try {
				BSBlueprintString blueprintString = null;
				if (result.encodedData.isPresent()) {
					blueprintString = BSBlueprintString.decode(result.encodedData.get());
				} else if (result.decodedData.isPresent()) {
					blueprintString = new BSBlueprintString(result.decodedData.get(),
							result.decodedData.get().toString(2));
				} else if (result.failureCause.isPresent()) {
					results.add(new FindBlueprintResult(Optional.empty(), Optional.of(result.failureCause.get())));
					continue;
				}
				results.add(new FindBlueprintResult(Optional.of(blueprintString), Optional.empty()));
			} catch (IllegalArgumentException | IOException e) {
				results.add(new FindBlueprintResult(Optional.empty(), Optional.of(e)));
			}
		}
		return results;
	}

	public static List<FindBlueprintRawResult> searchRaw(String content) {
		List<FindBlueprintRawResult> results = new ArrayList<>();
		findBlueprints(new ByteArrayInputStream(content.getBytes()), results);
		findProviders(content, results);
		return results;
	}

	private BlueprintFinder() {
	}
}
