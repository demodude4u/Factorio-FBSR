package com.demod.fbsr.app;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.json.JSONObject;

import com.demod.factorio.Config;
import com.demod.factorio.Utils;
import com.demod.fbsr.Blueprint;
import com.demod.fbsr.BlueprintFinder;
import com.demod.fbsr.BlueprintStringData;
import com.demod.fbsr.FBSR;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.TaskReporting;
import com.demod.fbsr.WebUtils;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.Uninterruptibles;

import javafx.util.Pair;
import net.dean.jraw.ApiException;
import net.dean.jraw.RedditClient;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.managers.AccountManager;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.CommentStream;
import net.dean.jraw.paginators.Sorting;
import net.dean.jraw.paginators.SubredditPaginator;
import net.dean.jraw.paginators.TimePeriod;

public class BlueprintBotRedditService extends AbstractScheduledService {

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

	private byte[] generateRedditFriendlyPNGImage(BufferedImage image) throws IOException {
		byte[] imageData;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ImageIO.write(image, "PNG", baos);
			imageData = baos.toByteArray();
		}
		if (imageData.length > 10000000) {
			return generateRedditFriendlyPNGImage(
					RenderUtils.scaleImage(image, image.getWidth() / 2, image.getHeight() / 2));
		}
		return imageData;
	}

	private Optional<Comment> getMyReply(CommentNode comments) {
		return comments.getChildren().stream().map(c -> c.getComment()).filter(c -> c.getAuthor().equals(myUserName))
				.findAny();
	}

	private JSONObject getOrCreateCache() throws FileNotFoundException, IOException {
		if (CACHE_FILE.exists()) {
			try (FileInputStream fis = new FileInputStream(CACHE_FILE)) {
				return Utils.readJsonFromStream(fis);
			}
		} else {
			JSONObject cache = new JSONObject();
			cache.put("lastProcessedSubmissionMillis", 0L);
			cache.put("lastProcessedCommentMillis", 0L);
			cache.put("lastProcessedMessageMillis", 0L);
			return cache;
		}
	}

	private String getPermaLink(Comment comment) {
		try {
			return "http://www.reddit.com/r/" + comment.getSubredditName() + "/comments/"
					+ comment.getSubmissionId().split("_")[1] + "/_/" + comment.getId();
		} catch (Exception e) {
			return "!!! Failed to create permalink! " + comment.getSubmissionId() + " !!!";
		}
	}

	private Optional<String> processContent(String content, String link, String subreddit, String author) {
		if (!content.contains(configJson.getString("summon_keyword"))) {
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
				try {
					BufferedImage image = FBSR.renderBlueprint(blueprint, reporting);
					reporting.addImage(WebUtils
							.uploadToHostingService("blueprint.png", generateRedditFriendlyPNGImage(image)).toString());
				} catch (Exception e) {
					reporting.addException(e);
				}
			}
		} catch (Exception e) {
			reporting.addException(e);
		}

		List<String> lines = new ArrayList<>();
		List<String> images = reporting.getImages();
		if (images.size() > 1) {
			int id = 1;
			for (String url : images) {
				lines.add("[Blueprint Image " + (id++) + "](" + url + ")");
			}
		} else if (!images.isEmpty()) {
			lines.add("[Blueprint Image](" + images.get(0) + ")");
		}

		for (String info : reporting.getInfo()) {
			lines.add("    " + info);
		}

		if (images.isEmpty()) {
			lines.add("    I can't seem to find any blueprints...");
		}
		if (!reporting.getExceptions().isEmpty()) {
			lines.add(
					"    There was a problem completing your request. I have contacted my programmer to fix it for you!");
		}

		ServiceFinder.findService(BlueprintBotDiscordService.class)
				.ifPresent(s -> s.sendReport("Reddit / " + subreddit + " / " + author, REDDIT_AUTHOR_URL, reporting));

		return Optional.of(lines.stream().collect(Collectors.joining("\n\n")));
	}

	private boolean processNewComments(JSONObject cacheJson, String subreddit, long ageLimitMillis)
			throws NetworkException, ApiException {
		long lastProcessedMillis = cacheJson.getLong("lastProcessedCommentMillis");

		CommentStream commentStream = new CommentStream(reddit, subreddit);
		commentStream.setTimePeriod(TimePeriod.ALL);
		commentStream.setSorting(Sorting.NEW);

		int processedCount = 0;
		long newestMillis = lastProcessedMillis;
		List<Pair<Comment, String>> pendingReplies = new LinkedList<>();
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
						comment.getSubredditName(), comment.getAuthor());
				if (response.isPresent()) {
					pendingReplies.add(new Pair<>(comment, response.get()));
				}
			}
		}
		for (Pair<Comment, String> pair : pendingReplies) {
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

	private boolean processNewMessages(JSONObject cacheJson, long ageLimitMillis) {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean processNewSubmissions(JSONObject cacheJson, String subreddit, long ageLimitMillis)
			throws NetworkException, ApiException {
		long lastProcessedMillis = cacheJson.getLong("lastProcessedSubmissionMillis");

		SubredditPaginator paginator = new SubredditPaginator(reddit, subreddit);
		paginator.setTimePeriod(TimePeriod.ALL);
		paginator.setSorting(Sorting.NEW);

		int processedCount = 0;
		long newestMillis = lastProcessedMillis;
		List<Pair<Submission, String>> pendingReplies = new LinkedList<>();
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
						submission.getSubredditName(), submission.getAuthor());
				if (response.isPresent()) {
					pendingReplies.add(new Pair<>(submission, response.get()));
				}
			}
		}
		for (Pair<Submission, String> pair : pendingReplies) {
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

	@Override
	protected void runOneIteration() throws Exception {
		try {
			JSONObject cacheJson = getOrCreateCache();
			boolean cacheUpdated = false;

			ensureConnectedToReddit();
			cacheUpdated |= processNewSubmissions(cacheJson, subreddit, ageLimitMillis);
			cacheUpdated |= processNewComments(cacheJson, subreddit, ageLimitMillis);
			cacheUpdated |= processNewMessages(cacheJson, ageLimitMillis);

			if (cacheUpdated) {
				saveCache(cacheJson);
			}

			ServiceFinder.findService(WatchdogService.class).ifPresent(watchdog -> {
				watchdog.notifyActive("Reddit Bot");
			});
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
		try {
			reddit = new RedditClient(UserAgent.of("server", "com.demod.fbsr", "0.0.1", "demodude4u"));
			account = new AccountManager(reddit);

			configJson = Config.get().getJSONObject("reddit");
			subreddit = configJson.getString("subreddit");
			ageLimitMillis = configJson.getInt("age_limit_hours") * 60 * 60 * 1000;

			JSONObject redditCredentialsJson = configJson.getJSONObject("credentials");
			credentials = Credentials.script( //
					redditCredentialsJson.getString("username"), //
					redditCredentialsJson.getString("password"), //
					redditCredentialsJson.getString("client_id"), //
					redditCredentialsJson.getString("client_secret") //
			);

			myUserName = redditCredentialsJson.getString("username");

			ServiceFinder.addService(this);
		} catch (Exception e) {
			e.printStackTrace();
			Throwables.throwIfUnchecked(e);
			throw new RuntimeException(e);
		}
	}
}
