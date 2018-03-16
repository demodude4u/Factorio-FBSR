package com.demod.fbsr.app;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.json.JSONObject;

import com.demod.factorio.Config;
import com.demod.factorio.Utils;
import com.demod.fbsr.Blueprint;
import com.demod.fbsr.BlueprintFinder;
import com.demod.fbsr.BlueprintStringData;
import com.demod.fbsr.FBSR;
import com.demod.fbsr.TaskReporting;
import com.demod.fbsr.WebUtils;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.Uninterruptibles;

import net.dean.jraw.ApiException;
import net.dean.jraw.RedditClient;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthException;
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

public class BlueprintBotRedditService extends AbstractScheduledService {

	private static final String WATCHDOG_LABEL = "Reddit Bot";

	private static final File CACHE_FILE = new File("redditCache.json");
	private static final String REDDIT_AUTHOR_URL = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/43/Reddit.svg/64px-Reddit.svg.png";

	private JSONObject configJson;
	private String myUserName;
	private String subreddit;
	private long ageLimitMillis;
	private Credentials credentials;

	private RedditClient reddit;
	private AccountManager account;
	private OAuthData authData;

	private long authExpireMillis = 0;
	private boolean processMessages;
	private String summonKeyword;
	private String myUserNameMention;

	private void ensureConnectedToReddit() throws NetworkException, OAuthException, InterruptedException {
		if (System.currentTimeMillis() + 60000 > authExpireMillis) {
			for (int wait = 4000; true; wait = Math.min(wait * 2, (5) * 60 * 1000)) {
				try {
					System.out.println("Connecting to Reddit...");
					authData = reddit.getOAuthHelper().easyAuth(credentials);
					authExpireMillis = authData.getExpirationDate().getTime();
					reddit.authenticate(authData);
					System.out.println("Reconnected to Reddit!");
					break;
				} catch (Exception e) {
					System.out.println("[Waiting " + TimeUnit.MILLISECONDS.toSeconds(wait)
							+ " seconds] Connection Failure [" + e.getClass().getSimpleName() + "]: " + e.getMessage());
					Thread.sleep(wait);
				}
			}
		}
	}

	private Optional<Comment> getMyReply(CommentNode comments) {
		return comments.getChildren().stream().map(c -> c.getComment()).filter(c -> c.getAuthor().equals(myUserName))
				.findAny();
	}

	private JSONObject getOrCreateCache() throws FileNotFoundException, IOException {
		if (CACHE_FILE.exists()) {
			try (FileInputStream fis = new FileInputStream(CACHE_FILE)) {
				return Utils.readJsonFromStream(fis);
			} catch (Exception e) {
				// No worries if anything went wrong with the file.
			}
		}

		JSONObject cache = new JSONObject();
		cache.put("lastProcessedSubmissionMillis", 0L);
		cache.put("lastProcessedCommentMillis", 0L);
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

	private Optional<String> processContent(String content, String link, String category, String author,
			Optional<WatchdogService> watchdog) {
		String contentLowerCase = content.toLowerCase();
		if (!contentLowerCase.contains(summonKeyword) && !contentLowerCase.contains(myUserNameMention)) {
			return Optional.empty();
		}

		TaskReporting reporting = new TaskReporting();
		reporting.setContext(content);
		reporting.addLink(link);

		try {
			List<BlueprintStringData> blueprintStrings = BlueprintFinder.search(content, reporting);
			List<Blueprint> blueprints = blueprintStrings.stream().flatMap(s -> s.getBlueprints().stream())
					.collect(Collectors.toList());

			for (Blueprint blueprint : blueprints) {
				watchdog.ifPresent(w -> w.notifyActive(WATCHDOG_LABEL));
				try {
					BufferedImage image = FBSR.renderBlueprint(blueprint, reporting);
					reporting.addImage(blueprint.getLabel(),
							WebUtils.uploadToHostingService("blueprint.png", image).toString());
				} catch (Exception e) {
					reporting.addException(e);
				}
			}
		} catch (Exception e) {
			reporting.addException(e);
		}

		List<String> lines = new ArrayList<>();
		List<Entry<Optional<String>, String>> images = reporting.getImages();
		if (images.size() > 1) {
			int id = 1;
			List<Entry<URL, String>> links = new ArrayList<>();
			for (Entry<Optional<String>, String> pair : images) {
				Optional<String> label = pair.getKey();
				String url = pair.getValue();
				try {
					links.add(new SimpleEntry<>(new URL(url), label.orElse(null)));
				} catch (MalformedURLException e) {
					reporting.addException(e);
				}
			}
			Optional<URL> albumUrl;
			try {
				albumUrl = Optional
						.of(WebUtils.uploadToBundly("Blueprint Images", "Renderings provided by Blueprint Bot", links));
			} catch (IOException e) {
				reporting.addException(e);
				albumUrl = Optional.empty();
			}
			if (albumUrl.isPresent()) {
				lines.add("Blueprint Images ([View As Album](" + albumUrl.get() + ")):\n");
			} else {
				lines.add("Blueprint Images:");
			}
			for (Entry<Optional<String>, String> pair : images) {
				Optional<String> label = pair.getKey();
				String url = pair.getValue();
				lines.add("[" + (id++) + ": " + label.orElse("Blueprint") + "](" + url + ")");
			}
		} else if (!images.isEmpty()) {
			Entry<Optional<String>, String> pair = images.get(0);
			Optional<String> label = pair.getKey();
			String url = pair.getValue();
			lines.add("[Blueprint Image" + label.map(s -> " (" + s + ")").orElse("") + "](" + url + ")");
		}

		for (String info : reporting.getInfo()) {
			lines.add("    " + info);
		}

		if (!reporting.getExceptions().isEmpty()) {
			lines.add(
					"    There was a problem completing your request. I have contacted my programmer to fix it for you!");
		}

		ServiceFinder.findService(BlueprintBotDiscordService.class)
				.ifPresent(s -> s.sendReport("Reddit / " + category + " / " + author, REDDIT_AUTHOR_URL, reporting));

		if (!lines.isEmpty()) {
			return Optional.of(lines.stream().collect(Collectors.joining("\n\n")));
		} else {
			return Optional.empty();
		}
	}

	private boolean processNewComments(JSONObject cacheJson, String subreddit, long ageLimitMillis,
			Optional<WatchdogService> watchdog) throws ApiException {
		long lastProcessedMillis = cacheJson.getLong("lastProcessedCommentMillis");

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

				Optional<String> response = processContent(comment.getBody(), getPermaLink(comment),
						comment.getSubredditName(), comment.getAuthor(), watchdog);
				if (response.isPresent()) {
					pendingReplies.add(new SimpleEntry<>(comment, response.get()));
				}
			}
		}
		for (Entry<Comment, String> pair : pendingReplies) {
			System.out.println("IM TRYING TO REPLY TO A COMMENT!");
			while (true) {
				try {
					account.reply(pair.getKey(), pair.getValue());
					break;
				} catch (ApiException e) {
					if (e.getReason().equals("RATELIMIT")) {
						System.out.println("RATE LIMITED! WAITING 6 MINUTES...");
						Uninterruptibles.sleepUninterruptibly(6, TimeUnit.MINUTES);
					} else {
						throw e;
					}
				}
			}
		}

		if (processedCount > 0) {
			System.out.println("Processed " + processedCount + " comment(s) from /r/" + subreddit);
			cacheJson.put("lastProcessedCommentMillis", newestMillis);
			return true;
		} else {
			return false;
		}
	}

	private boolean processNewMessages(JSONObject cacheJson, long ageLimitMillis, Optional<WatchdogService> watchdog)
			throws ApiException {

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

				Optional<String> response = processContent(message.getBody(), getPermaLink(message), "(Private)",
						message.getAuthor(), watchdog);
				if (response.isPresent()) {
					pendingReplies.add(new SimpleEntry<>(message, response.get()));
				}
			}
		}

		if (!processedMessages.isEmpty()) {
			new InboxManager(reddit).setRead(true, processedMessages.get(0),
					processedMessages.stream().skip(1).toArray(Message[]::new));
		}

		for (Entry<Message, String> pair : pendingReplies) {
			System.out.println("IM TRYING TO REPLY TO A MESSAGE!");
			while (true) {
				try {
					account.reply(pair.getKey(), pair.getValue());
					break;
				} catch (ApiException e) {
					if (e.getReason().equals("RATELIMIT")) {
						System.out.println("RATE LIMITED! WAITING 6 MINUTES...");
						Uninterruptibles.sleepUninterruptibly(6, TimeUnit.MINUTES);
					} else {
						throw e;
					}
				}
			}
		}

		if (processedCount > 0) {
			System.out.println("Processed " + processedCount + " message(s)");
			cacheJson.put("lastProcessedMessageMillis", newestMillis);
			return true;
		} else {
			return false;
		}
	}

	private boolean processNewSubmissions(JSONObject cacheJson, String subreddit, long ageLimitMillis,
			Optional<WatchdogService> watchdog) throws NetworkException, ApiException {
		long lastProcessedMillis = cacheJson.getLong("lastProcessedSubmissionMillis");

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

				Optional<String> response = processContent(submission.getSelftext(), submission.getUrl(),
						submission.getSubredditName(), submission.getAuthor(), watchdog);
				if (response.isPresent()) {
					pendingReplies.add(new SimpleEntry<>(submission, response.get()));
				}
			}
		}
		for (Entry<Submission, String> pair : pendingReplies) {
			System.out.println("IM TRYING TO REPLY TO A SUBMISSION!");
			while (true) {
				try {
					account.reply(pair.getKey(), pair.getValue());
					break;
				} catch (ApiException e) {
					if (e.getReason().equals("RATELIMIT")) {
						System.out.println("RATE LIMITED! WAITING 6 MINUTES...");
						Uninterruptibles.sleepUninterruptibly(6, TimeUnit.MINUTES);
					} else {
						throw e;
					}
				}
			}
		}

		if (processedCount > 0) {
			System.out.println("Processed " + processedCount + " submission(s) from /r/" + subreddit);
			cacheJson.put("lastProcessedSubmissionMillis", newestMillis);
			return true;
		} else {
			return false;
		}
	}

	public void processRequest(String... ids) throws NetworkException, ApiException {
		System.out.println("REQUESTED: " + Arrays.toString(ids));
		Listing<Thing> listing = reddit.get(ids);
		System.out.println("REQUESTED RESULT = " + listing.size());
		for (Thing thing : listing) {
			if (thing instanceof Comment) {
				System.out.println("REQUESTED COMMENT!");
				Comment comment = (Comment) thing;
				Optional<String> response = processContent(comment.getBody(), getPermaLink(comment),
						comment.getSubredditName(), comment.getAuthor(), Optional.empty());
				if (response.isPresent()) {
					account.reply(comment, response.get());
				}
			} else if (thing instanceof Submission) {
				System.out.println("REQUESTED SUBMISSION!");
				Submission submission = (Submission) thing;
				Optional<String> response = processContent(submission.getSelftext(), submission.getUrl(),
						submission.getSubredditName(), submission.getAuthor(), Optional.empty());
				if (response.isPresent()) {
					account.reply(submission, response.get());
				}
			}
		}
	}

	@Override
	protected void runOneIteration() throws Exception {
		Optional<WatchdogService> watchdog = ServiceFinder.findService(WatchdogService.class);
		watchdog.ifPresent(w -> w.notifyKnown(WATCHDOG_LABEL));

		try {
			JSONObject cacheJson = getOrCreateCache();
			boolean cacheUpdated = false;

			ensureConnectedToReddit();
			cacheUpdated |= processNewSubmissions(cacheJson, subreddit, ageLimitMillis, watchdog);
			cacheUpdated |= processNewComments(cacheJson, subreddit, ageLimitMillis, watchdog);
			if (processMessages) {
				cacheUpdated |= processNewMessages(cacheJson, ageLimitMillis, watchdog);
			}

			if (cacheUpdated) {
				saveCache(cacheJson);
			}

			watchdog.ifPresent(w -> w.notifyActive(WATCHDOG_LABEL));
		} catch (NetworkException e) {
			System.out.println("Network Problem [" + e.getClass().getSimpleName() + "]: " + e.getMessage());
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
	protected void shutDown() throws Exception {
		ServiceFinder.removeService(this);
		reddit.getOAuthHelper().revokeAccessToken(credentials);
		reddit.deauthenticate();
	}

	@Override
	protected void startUp() {
		reddit = new RedditClient(UserAgent.of("server", "com.demod.fbsr", "0.0.1", "demodude4u"));
		account = new AccountManager(reddit);

		configJson = Config.get().getJSONObject("reddit");
		subreddit = configJson.getString("subreddit");
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
