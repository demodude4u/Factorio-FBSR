package com.demod.fbsr.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.dcba.CommandReporting;
import com.demod.factorio.Config;
import com.demod.factorio.Utils;
import com.demod.fbsr.BlueprintFinder;
import com.demod.fbsr.BlueprintFinder.FindBlueprintResult;
import com.demod.fbsr.FBSR;
import com.demod.fbsr.RenderRequest;
import com.demod.fbsr.RenderResult;
import com.demod.fbsr.WebUtils;
import com.demod.fbsr.bs.BSBlueprint;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.Uninterruptibles;

import net.dean.jraw.ApiException;
import net.dean.jraw.RedditClient;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.managers.AccountManager;
import net.dean.jraw.managers.InboxManager;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Message;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.Thing;
import net.dean.jraw.paginators.CommentStream;
import net.dean.jraw.paginators.InboxPaginator;
import net.dean.jraw.paginators.Sorting;
import net.dean.jraw.paginators.SubredditPaginator;
import net.dean.jraw.paginators.TimePeriod;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;

public class BlueprintBotRedditService extends AbstractScheduledService {

	private static final String WATCHDOG_LABEL = "Reddit Bot";

	private static final File CACHE_FILE = new File("redditCache.json");
	private static final String REDDIT_AUTHOR_URL = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/43/Reddit.svg/64px-Reddit.svg.png";
	private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintBotRedditService.class);

	private JSONObject configJson;
	private String myUserName;
	private List<String> subreddits;
	private long ageLimitMillis;
	private Credentials credentials;

	private RedditClient reddit;
	private AccountManager account;
	private OAuthData authData;

	private long authExpireMillis = 0;
	private boolean processMessages;
	private String summonKeyword;
	private String myUserNameMention;

	private void ensureConnectedToReddit() throws NetworkException, InterruptedException {
		if (System.currentTimeMillis() + 60000 > authExpireMillis) {
			for (int wait = 4000; true; wait = Math.min(wait * 2, (5) * 60 * 1000)) {
				try {
					LOGGER.info("Connecting to Reddit...");
					authData = reddit.getOAuthHelper().easyAuth(credentials);
					authExpireMillis = authData.getExpirationDate().getTime();
					reddit.authenticate(authData);
					LOGGER.info("Reconnected to Reddit!");
					break;
				} catch (Exception e) {
					LOGGER.info("[Waiting {} seconds] Connection Failure [{}]: {}",
							TimeUnit.MILLISECONDS.toSeconds(wait), e.getClass().getSimpleName(), e.getMessage());
					Thread.sleep(wait);
				}
			}
		}
	}

	private Optional<Comment> getMyReply(CommentNode comments) {
		return comments.getChildren().stream().map(c -> c.getComment()).filter(c -> c.getAuthor().equals(myUserName))
				.findAny();
	}

	private JSONObject getOrCreateCache() {
		if (CACHE_FILE.exists()) {
			try (FileInputStream fis = new FileInputStream(CACHE_FILE)) {
				return Utils.readJsonFromStream(fis);
			} catch (Exception e) {
				// No worries if anything went wrong with the file.
			}
		}

		JSONObject cache = new JSONObject();
		cache.put("lastProcessedMessageMillis", 0L);
		return cache;
	}

	private String getPermaLink(Comment comment) {
		try {
			return "http://www.reddit.com/r/" + comment.getSubredditName() + "/comments/"
					+ comment.getSubmissionId().split("_")[1] + "/_/" + comment.getId();
		} catch (Exception e) {
			return "!!! Failed to create permalink! " + comment.getSubmissionId() + " !!!";
		}
	}

	private String getPermaLink(Message message) {
		return "https://www.reddit.com/message/messages/" + message.getId();
	}

	private List<String> processContent(String content, String link, String category, String author,
			Optional<WatchdogService> watchdog) {
		String contentLowerCase = content.toLowerCase();
		if (!contentLowerCase.contains(summonKeyword) && !contentLowerCase.contains(myUserNameMention)) {
			return ImmutableList.of();
		}

		CommandReporting reporting = new CommandReporting("Reddit / " + category + " / " + author, REDDIT_AUTHOR_URL,
				Instant.now());
		reporting.setCommand(content);
		reporting.addField(new Field("Reddit Link", link, false));

		List<String> infos = new ArrayList<>();
		List<Entry<Optional<String>, String>> imageLinks = new ArrayList<>();

		try {
			List<FindBlueprintResult> blueprintStrings = BlueprintFinder.search(content);
			blueprintStrings.forEach(f -> f.failureCause.ifPresent(e -> reporting.addException(e)));
			List<BSBlueprint> blueprints = blueprintStrings.stream().filter(f -> f.blueprintString.isPresent())
					.flatMap(f -> f.blueprintString.get().findAllBlueprints().stream()).collect(Collectors.toList());
			List<Long> renderTimes = new ArrayList<>();

			for (BSBlueprint blueprint : blueprints) {
				watchdog.ifPresent(w -> w.notifyActive(WATCHDOG_LABEL));
				try {
					RenderRequest request = new RenderRequest(blueprint, reporting);
					RenderResult result = FBSR.renderBlueprint(request);
					imageLinks.add(new SimpleEntry<>(blueprint.label,
							WebUtils.uploadToImgBB(result.image, blueprint.label.orElse("Untitled Blueprint"))));
					renderTimes.add(result.renderTime);
				} catch (Exception e) {
					reporting.addException(e);
				}
			}

			if (!renderTimes.isEmpty()) {
				reporting.addField(new Field("Render Time", renderTimes.stream().mapToLong(l -> l).sum() + " ms"
						+ (renderTimes.size() > 1
								? (" [" + renderTimes.stream().map(Object::toString).collect(Collectors.joining(", "))
										+ "]")
								: ""),
						true));
			}
		} catch (Exception e) {
			reporting.addException(e);
		}

		List<String> lines = new ArrayList<>();
		if (imageLinks.size() > 1) {
			int id = 1;
			List<Entry<URL, String>> links = new ArrayList<>();
			for (Entry<Optional<String>, String> pair : imageLinks) {
				Optional<String> label = pair.getKey();
				String url = pair.getValue();
				try {
					links.add(new SimpleEntry<>(new URL(url), label.orElse(null)));
				} catch (MalformedURLException e) {
					reporting.addException(e);
				}
			}

			lines.add("Blueprint Images:");

			for (Entry<Optional<String>, String> pair : imageLinks) {
				Optional<String> label = pair.getKey();
				String url = pair.getValue();
				lines.add("[" + (id++) + ": " + label.orElse("Blueprint") + "](" + url + ")");
			}
		} else if (!imageLinks.isEmpty()) {
			Entry<Optional<String>, String> pair = imageLinks.get(0);
			Optional<String> label = pair.getKey();
			String url = pair.getValue();
			lines.add("[Blueprint Image" + label.map(s -> " (" + s + ")").orElse("") + "](" + url + ")");
			reporting.setImageURL(url);
		}

		for (String info : infos) {
			lines.add("    " + info);
		}

		if (!reporting.getExceptionsWithBlame().isEmpty()) {
			lines.add("    Sorry, There was a problem completing your request.");
		}

		ServiceFinder.findService(BlueprintBotDiscordService.class).ifPresent(s -> s.getBot().submitReport(reporting));

		if (!lines.isEmpty()) {
			List<String> res = new ArrayList<>();
			String message = "";
			for (int i = 0; i < lines.size(); i++) {
				String line = lines.get(i) + "\n\n";
				if (message.length() + line.length() < 10000) {
					message += line;
				} else {
					res.add(message);
					message = "Blueprint Images (Continued):\n\n";
				}
			}
			res.add(message);
			return res;
		} else {
			return ImmutableList.of();
		}
	}

	private boolean processNewComments(JSONObject cacheJson, String subreddit, long ageLimitMillis,
			Optional<WatchdogService> watchdog) throws ApiException, IOException {
		long lastProcessedMillis = cacheJson.optLong("lastProcessedCommentMillis-" + subreddit);

		CommentStream commentStream = new CommentStream(reddit, subreddit);
		commentStream.setTimePeriod(TimePeriod.ALL);
		commentStream.setSorting(Sorting.NEW);

		int processedCount = 0;
		long newestMillis = lastProcessedMillis;
		List<Entry<Comment, String>> pendingReplies = new LinkedList<>();
		paginate: for (Listing<Comment> listing : commentStream) {
			for (Comment comment : listing) {
				long createMillis = comment.getCreated().getTime();
				if (createMillis <= lastProcessedMillis
						|| (System.currentTimeMillis() - createMillis > ageLimitMillis)) {
					break paginate;
				}
				processedCount++;
				newestMillis = Math.max(newestMillis, createMillis);

				if (comment.getAuthor().equals(myUserName)) {
					break paginate;
				}

				if (comment.isArchived()) {
					continue;
				}

				List<String> responses = processContent(comment.getBody(), getPermaLink(comment),
						comment.getSubredditName(), comment.getAuthor(), watchdog);
				for (String response : responses) {
					pendingReplies.add(new SimpleEntry<>(comment, response));
				}
			}
		}
		for (Entry<Comment, String> pair : pendingReplies) {
			LOGGER.info("IM TRYING TO REPLY TO A COMMENT!");
			String message = pair.getValue();
			if (message.length() > 10000) {

				// TODO links expire, need a new approach
				Optional<BlueprintBotDiscordService> discordService = ServiceFinder
						.findService(BlueprintBotDiscordService.class);
				message = discordService.get().useDiscordForFileHosting("MESSAGE_TOO_LONG.txt", message.getBytes())
						.toString();
			}

			while (true) {
				try {
					account.reply(pair.getKey(), message);
					break;
				} catch (ApiException e) {
					if (e.getReason().equals("RATELIMIT")) {
						LOGGER.info("RATE LIMITED! WAITING 6 MINUTES...");
						Uninterruptibles.sleepUninterruptibly(6, TimeUnit.MINUTES);
					} else {
						throw e;
					}
				}
			}
		}

		if (processedCount > 0) {
			LOGGER.info("Processed {} comment(s) from /r/{}", processedCount, subreddit);
			cacheJson.put("lastProcessedCommentMillis-" + subreddit, newestMillis);
			return true;
		} else {
			return false;
		}
	}

	private boolean processNewMessages(JSONObject cacheJson, long ageLimitMillis, Optional<WatchdogService> watchdog)
			throws ApiException, IOException {

		long lastProcessedMillis = cacheJson.getLong("lastProcessedMessageMillis");

		InboxPaginator paginator = new InboxPaginator(reddit, "messages");
		paginator.setTimePeriod(TimePeriod.ALL);
		paginator.setSorting(Sorting.NEW);

		int processedCount = 0;
		long newestMillis = lastProcessedMillis;
		List<Entry<Message, String>> pendingReplies = new LinkedList<>();
		List<Message> processedMessages = new LinkedList<>();
		paginate: for (Listing<Message> listing : paginator) {
			for (Message message : listing) {
				if (message.isRead()) {
					break paginate;
				}

				long createMillis = message.getCreated().getTime();
				if (createMillis <= lastProcessedMillis
						|| (System.currentTimeMillis() - createMillis > ageLimitMillis)) {
					break paginate;
				}

				processedCount++;
				newestMillis = Math.max(newestMillis, createMillis);
				processedMessages.add(message);

				List<String> responses = processContent(message.getBody(), getPermaLink(message), "(Private)",
						message.getAuthor(), watchdog);
				for (String response : responses) {
					pendingReplies.add(new SimpleEntry<>(message, response));
				}
			}
		}

		if (!processedMessages.isEmpty()) {
			new InboxManager(reddit).setRead(true, processedMessages.get(0),
					processedMessages.stream().skip(1).toArray(Message[]::new));
		}

		for (Entry<Message, String> pair : pendingReplies) {
			LOGGER.info("IM TRYING TO REPLY TO A MESSAGE!");
			String message = pair.getValue();
			if (message.length() > 10000) {
				// TODO links expire, need a new approach
				Optional<BlueprintBotDiscordService> discordService = ServiceFinder
						.findService(BlueprintBotDiscordService.class);
				message = discordService.get().useDiscordForFileHosting("MESSAGE_TOO_LONG.txt", message.getBytes())
						.toString();
			}

			while (true) {
				try {
					account.reply(pair.getKey(), message);
					break;
				} catch (ApiException e) {
					if (e.getReason().equals("RATELIMIT")) {
						LOGGER.info("RATE LIMITED! WAITING 6 MINUTES...");
						Uninterruptibles.sleepUninterruptibly(6, TimeUnit.MINUTES);
					} else {
						throw e;
					}
				}
			}
		}

		if (processedCount > 0) {
			LOGGER.info("Processed {} message(s)", processedCount);
			cacheJson.put("lastProcessedMessageMillis", newestMillis);
			return true;
		} else {
			return false;
		}
	}

	private boolean processNewSubmissions(JSONObject cacheJson, String subreddit, long ageLimitMillis,
			Optional<WatchdogService> watchdog) throws NetworkException, ApiException, IOException {
		long lastProcessedMillis = cacheJson.optLong("lastProcessedSubmissionMillis-" + subreddit);

		SubredditPaginator paginator = new SubredditPaginator(reddit, subreddit);
		paginator.setTimePeriod(TimePeriod.ALL);
		paginator.setSorting(Sorting.NEW);

		int processedCount = 0;
		long newestMillis = lastProcessedMillis;
		List<Entry<Submission, String>> pendingReplies = new LinkedList<>();
		paginate: for (Listing<Submission> listing : paginator) {
			for (Submission submission : listing) {
				long createMillis = submission.getCreated().getTime();
				if (createMillis <= lastProcessedMillis
						|| (System.currentTimeMillis() - createMillis > ageLimitMillis)) {
					break paginate;
				}
				processedCount++;
				newestMillis = Math.max(newestMillis, createMillis);

				if (!submission.isSelfPost() || submission.isLocked() || submission.isArchived()) {
					continue;
				}

				CommentNode comments = submission.getComments();
				if (comments == null && submission.getCommentCount() > 0) {
					submission = reddit.getSubmission(submission.getId());
					comments = submission.getComments();
				}
				if (comments != null && getMyReply(comments).isPresent()) {
					break paginate;
				}

				List<String> responses = processContent(submission.getSelftext(), submission.getUrl(),
						submission.getSubredditName(), submission.getAuthor(), watchdog);
				for (String response : responses) {
					pendingReplies.add(new SimpleEntry<>(submission, response));
				}
			}
		}
		for (Entry<Submission, String> pair : pendingReplies) {
			LOGGER.info("IM TRYING TO REPLY TO A SUBMISSION!");
			String message = pair.getValue();
			if (message.length() > 10000) {
				// TODO links expire, need a new approach
				Optional<BlueprintBotDiscordService> discordService = ServiceFinder
						.findService(BlueprintBotDiscordService.class);
				message = discordService.get().useDiscordForFileHosting("MESSAGE_TOO_LONG.txt", message.getBytes())
						.toString();
			}

			while (true) {
				try {
					account.reply(pair.getKey(), message);
					break;
				} catch (ApiException e) {
					if (e.getReason().equals("RATELIMIT")) {
						LOGGER.info("RATE LIMITED! WAITING 6 MINUTES...");
						Uninterruptibles.sleepUninterruptibly(6, TimeUnit.MINUTES);
					} else {
						throw e;
					}
				}
			}
		}

		if (processedCount > 0) {
			LOGGER.info("Processed {} submission(s) from /r/{}", processedCount, subreddit);
			cacheJson.put("lastProcessedSubmissionMillis-" + subreddit, newestMillis);
			return true;
		} else {
			return false;
		}
	}

	public void processRequest(String... ids) throws NetworkException, ApiException {
		LOGGER.info("REQUESTED: {}", Arrays.toString(ids));
		Listing<Thing> listing = reddit.get(ids);
		LOGGER.info("REQUESTED RESULT = {}", listing.size());
		for (Thing thing : listing) {
			if (thing instanceof Comment) {
				LOGGER.info("REQUESTED COMMENT!");
				Comment comment = (Comment) thing;
				List<String> responses = processContent(comment.getBody(), getPermaLink(comment),
						comment.getSubredditName(), comment.getAuthor(), Optional.empty());
				for (String response : responses) {
					account.reply(comment, response);
				}
			} else if (thing instanceof Submission) {
				LOGGER.info("REQUESTED SUBMISSION!");
				Submission submission = (Submission) thing;
				List<String> responses = processContent(submission.getSelftext(), submission.getUrl(),
						submission.getSubredditName(), submission.getAuthor(), Optional.empty());
				for (String response : responses) {
					account.reply(submission, response);
				}
			}
		}
	}

	@Override
	protected void runOneIteration() {
		Optional<WatchdogService> watchdog = ServiceFinder.findService(WatchdogService.class);
		watchdog.ifPresent(w -> w.notifyKnown(WATCHDOG_LABEL));

		try {
			JSONObject cacheJson = getOrCreateCache();
			boolean cacheUpdated = false;

			ensureConnectedToReddit();

			for (String subreddit : subreddits) {
				cacheUpdated |= processNewSubmissions(cacheJson, subreddit, ageLimitMillis, watchdog);
				cacheUpdated |= processNewComments(cacheJson, subreddit, ageLimitMillis, watchdog);
			}

			if (processMessages) {
				cacheUpdated |= processNewMessages(cacheJson, ageLimitMillis, watchdog);
			}

			if (cacheUpdated) {
				saveCache(cacheJson);
			}

			watchdog.ifPresent(w -> w.notifyActive(WATCHDOG_LABEL));
		} catch (NetworkException e) {
			LOGGER.info("Network Problem [{}]: {}", e.getClass().getSimpleName(), e.getMessage());
			authExpireMillis = 0;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void saveCache(JSONObject cacheJson) throws IOException {
		try (FileWriter fw = new FileWriter(CACHE_FILE)) {
			fw.write(cacheJson.toString(2));
		}
	}

	@Override
	protected Scheduler scheduler() {
		return Scheduler.newFixedDelaySchedule(0, configJson.getInt("refresh_seconds"), TimeUnit.SECONDS);
	}

	@Override
	protected void shutDown() {
		ServiceFinder.removeService(this);
		reddit.getOAuthHelper().revokeAccessToken(credentials);
		reddit.deauthenticate();
	}

	@Override
	protected void startUp() throws JSONException, IOException {

		if (!FBSR.initialize()) {
			throw new RuntimeException("Failed to initialize FBSR.");
		}

		reddit = new RedditClient(UserAgent.of("server", "com.demod.fbsr", "0.0.1", "demodude4u"));
		account = new AccountManager(reddit);

		configJson = Config.get().getJSONObject("reddit");
		if (configJson.has("subreddit")) {
			subreddits = ImmutableList.of(configJson.getString("subreddit"));
		} else {
			JSONArray subredditsJson = configJson.getJSONArray("subreddits");
			subreddits = new ArrayList<>();
			Utils.<String>forEach(subredditsJson, s -> subreddits.add(s));
		}
		ageLimitMillis = configJson.getInt("age_limit_hours") * 60 * 60 * 1000;
		processMessages = configJson.getBoolean("process_messages");
		summonKeyword = configJson.getString("summon_keyword").toLowerCase();

		JSONObject redditCredentialsJson = configJson.getJSONObject("credentials");
		credentials = Credentials.script( //
				redditCredentialsJson.getString("username"), //
				redditCredentialsJson.getString("password"), //
				redditCredentialsJson.getString("client_id"), //
				redditCredentialsJson.getString("client_secret") //
		);

		myUserName = redditCredentialsJson.getString("username");
		myUserNameMention = ("u/" + myUserName).toLowerCase();

		ServiceFinder.addService(this);
	}
}
