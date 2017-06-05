package com.demod.fbsr.app;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import com.demod.dcba.CommandHandler;
import com.demod.dcba.DCBA;
import com.demod.dcba.DiscordBot;
import com.demod.fbsr.Blueprint;
import com.demod.fbsr.BlueprintReporting;
import com.demod.fbsr.BlueprintStringData;
import com.demod.fbsr.FBSR;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.WebUtils;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.WorldMap.Debug;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.util.concurrent.AbstractIdleService;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Message.Attachment;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class BlueprintBotDiscordService extends AbstractIdleService {

	private static final String USERID_DEMOD = "100075603016814592";

	private static final int BP_MAX_SIZE = 4000000;

	private static final Pattern blueprintPattern = Pattern.compile("([0-9][A-Za-z0-9+\\/=\\r\\n]{90,})");

	private static final Pattern debugPattern = Pattern.compile("DEBUG:([A-Za-z0-9_]+)");

	private static final Pattern pastebinPattern = Pattern.compile("pastebin\\.com/([A-Za-z0-9]+)");
	private static final Pattern hastebinPattern = Pattern.compile("hastebin\\.com/([A-Za-z0-9]+)");
	private static final Pattern gistPattern = Pattern.compile("gist\\.github\\.com/([-a-zA-Z0-9@:%_\\+.~#?&//=]+)");
	private static final Pattern gitlabSnippetPattern = Pattern.compile("gitlab\\.com/snippets/([A-Za-z0-9]+)");

	public static void main(String[] args) {
		new BlueprintBotDiscordService().startAsync();
	}

	private DiscordBot bot;

	private void findBlueprints(MessageReceivedEvent event, BlueprintReporting reporting, String content) {
		Matcher matcher = blueprintPattern.matcher(content);
		while (matcher.find()) {
			String blueprintString = matcher.group();

			reporting.addContext(blueprintString);

			try {
				event.getChannel().sendTyping().complete();

				BlueprintStringData blueprintStringData = new BlueprintStringData(blueprintString);
				System.out.println("Parsing blueprints: " + blueprintStringData.getBlueprints().size());
				if (blueprintStringData.getBlueprints().size() == 1) {
					BufferedImage image = FBSR.renderBlueprint(blueprintStringData.getBlueprints().get(0), reporting);
					byte[] imageData = generateDiscordFriendlyPNGImage(image);
					Message message = event.getChannel()
							.sendFile(new ByteArrayInputStream(imageData), "blueprint.png", null).complete();
					reporting.addImageURL(message.getAttachments().get(0).getUrl());
				} else {
					try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
							ZipOutputStream zos = new ZipOutputStream(baos)) {

						int counter = 1;
						for (Blueprint blueprint : blueprintStringData.getBlueprints()) {
							try {
								BufferedImage image = FBSR.renderBlueprint(blueprint, reporting);
								zos.putNextEntry(new ZipEntry("blueprint " + counter + ".png"));
								ImageIO.write(image, "PNG", zos);
							} catch (Exception e) {
								reporting.addException(e);
							}
							counter++;
						}

						zos.close();
						byte[] zipData = baos.toByteArray();
						Message message = event.getChannel()
								.sendFile(new ByteArrayInputStream(zipData), "blueprint book images.zip", null)
								.complete();
						reporting.addDownloadURL(message.getAttachments().get(0).getUrl());
					}
				}
			} catch (Exception e) {
				reporting.addException(e);
			}
		}
	}

	private void findBlueprintsInURL(MessageReceivedEvent event, BlueprintReporting reporting, String url) {
		StringBuilder contentBuilder = new StringBuilder();
		reporting.addContext(url);
		try {
			URLConnection hc = new URL(url).openConnection();
			hc.setRequestProperty("User-Agent",
					"Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");

			try (BufferedReader br = new BufferedReader(
					new InputStreamReader(WebUtils.limitMaxBytes(hc.getInputStream(), BP_MAX_SIZE)))) {
				String line;
				while ((line = br.readLine()) != null) {
					contentBuilder.append(line).append('\n');
				}
				findBlueprints(event, reporting, contentBuilder.toString());
			}
		} catch (IOException e) {
			reporting.addException(e);
		}
	}

	private void findDebugOptions(BlueprintReporting reporting, String content) {
		Matcher matcher = debugPattern.matcher(content);
		while (matcher.find()) {
			String fieldName = matcher.group(1);
			try {
				Field field = WorldMap.Debug.class.getField(fieldName);
				if (!reporting.getDebug().isPresent()) {
					reporting.setDebug(Optional.of(new WorldMap.Debug()));
				}
				field.set(reporting.getDebug().get(), true);
				reporting.addWarning("Debug Enabled: " + fieldName);
			} catch (NoSuchFieldException e) {
				reporting.addWarning("Unknown Debug Option: " + fieldName);
			} catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
				reporting.addException(e);
			}
		}
	}

	private void findTextAttachments(MessageReceivedEvent event, BlueprintReporting reporting) {
		for (Attachment attachment : event.getMessage().getAttachments()) {
			if (attachment.getSize() < BP_MAX_SIZE) {
				findBlueprintsInURL(event, reporting, attachment.getUrl());
			}
		}
	}

	private void findTextHostingSites(MessageReceivedEvent event, BlueprintReporting reporting, String content) {
		{
			Matcher matcher = pastebinPattern.matcher(content);
			while (matcher.find()) {
				String code = matcher.group(1);
				findBlueprintsInURL(event, reporting, "https://pastebin.com/raw/" + code);
			}
		}
		{
			Matcher matcher = hastebinPattern.matcher(content);
			while (matcher.find()) {
				String code = matcher.group(1);
				findBlueprintsInURL(event, reporting, "https://hastebin.com/raw/" + code);
			}
		}
		{
			Matcher matcher = gitlabSnippetPattern.matcher(content);
			while (matcher.find()) {
				String code = matcher.group(1);
				findBlueprintsInURL(event, reporting, "https://gitlab.com/snippets/" + code + "/raw");
			}
		}
		{
			Matcher matcher = gistPattern.matcher(content);
			while (matcher.find()) {
				String code = matcher.group(1);
				findBlueprintsInURL(event, reporting, "https://gist.githubusercontent.com/" + code + "/raw/");
			}
		}
	}

	private byte[] generateDiscordFriendlyPNGImage(BufferedImage image) throws IOException {
		byte[] imageData;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ImageIO.write(image, "PNG", baos);
			imageData = baos.toByteArray();
		}
		if (imageData.length > 8000000) {
			return generateDiscordFriendlyPNGImage(
					RenderUtils.scaleImage(image, image.getWidth() / 2, image.getHeight() / 2));
		}
		return imageData;
	}

	private String getReadableAddress(MessageReceivedEvent event) {
		if (event.getGuild() == null) {
			return event.getAuthor().getName();
		} else {
			return event.getGuild().getName() + " / #" + event.getChannel().getName() + " / "
					+ event.getAuthor().getName();
		}
	}

	private void sendReportToDemod(MessageReceivedEvent event, BlueprintReporting reporting) {
		Optional<Debug> debug = reporting.getDebug();
		List<String> contexts = reporting.getContexts();
		List<Exception> exceptions = reporting.getExceptions();
		List<String> warnings = reporting.getWarnings();
		List<String> imageUrls = reporting.getImageURLs();
		List<String> downloadUrls = reporting.getDownloadURLs();

		if (!exceptions.isEmpty()) {
			event.getChannel()
					.sendMessage(
							"There was a problem completing your request. I have contacted my programmer to fix it for you!")
					.complete();
		}

		if (imageUrls.isEmpty() && downloadUrls.isEmpty()) {
			event.getChannel().sendMessage("I can't seem to find any blueprints. :frowning:").complete();
		}

		EmbedBuilder builder = new EmbedBuilder();
		builder.setAuthor(getReadableAddress(event), null, event.getAuthor().getEffectiveAvatarUrl());
		builder.setTimestamp(Instant.now());

		if (!warnings.isEmpty()) {
			builder.setColor(Color.orange);
		}
		if (debug.isPresent()) {
			builder.setColor(Color.magenta);
		}
		if (!exceptions.isEmpty()) {
			builder.setColor(Color.red);
		}

		boolean firstImage = true;
		for (String imageUrl : imageUrls) {
			if (firstImage) {
				firstImage = false;
				builder.setImage(imageUrl);
			} else {
				builder.addField("Additional Image", imageUrl, true);
			}
		}

		for (String downloadUrl : downloadUrls) {
			builder.addField("Download", downloadUrl, true);
		}

		if (!warnings.isEmpty()) {
			builder.addField("Warnings", warnings.stream().collect(Collectors.joining("\n")), false);
		}

		Multiset<String> uniqueExceptions = LinkedHashMultiset.create();
		for (Exception e : exceptions) {
			try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
				e.printStackTrace();
				e.printStackTrace(pw);
				pw.flush();
				uniqueExceptions.add(e.getClass().getSimpleName() + ": " + e.getMessage() + "#####"
						+ sw.toString().substring(0, MessageEmbed.VALUE_MAX_LENGTH));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		for (Entry<String> entry : uniqueExceptions.entrySet()) {
			String message = entry.getElement();
			String[] split = message.split("#####");
			builder.addField(split[0] + (entry.getCount() > 1 ? " (" + entry.getCount() + " times)" : ""), split[1],
					false);
		}

		List<String> contextFiles = new ArrayList<>();
		for (String context : contexts) {
			if (context.length() <= MessageEmbed.VALUE_MAX_LENGTH) {
				builder.addField("Context", context, false);
			} else {
				contextFiles.add(context);
			}
		}

		PrivateChannel privateChannel = event.getJDA().getUserById(USERID_DEMOD).openPrivateChannel().complete();
		privateChannel.sendMessage(builder.build()).complete();

		for (String context : contextFiles) {
			privateChannel.sendFile(new ByteArrayInputStream(context.getBytes()), "context.txt", null).complete();
		}
	}

	@Override
	protected void shutDown() throws Exception {
		bot.stopAsync().awaitTerminated();
	}

	@Override
	protected void startUp() throws Exception {
		bot = DCBA.builder()//
				.withCommandPrefix("-")
				//
				.setInfo("Blueprint Bot")//
				.withSupport("Find Demod and complain to him!")//
				.withTechnology("FBSR", "Factorio Blueprint String Renderer")//
				.withTechnology("FactorioDataWrapper", "Factorio Data Scraper")//
				.withCredits("Attribution", "Factorio - Made by Wube Software")//
				//
				.addCommand("blueprint", (CommandHandler) event -> {
					String content = event.getMessage().getStrippedContent();
					BlueprintReporting reporting = new BlueprintReporting();
					System.out.println("\n############################################################\n");
					findDebugOptions(reporting, content);
					findBlueprints(event, reporting, content);
					findTextHostingSites(event, reporting, content);
					findTextAttachments(event, reporting);
					sendReportToDemod(event, reporting);
				})//
				.withHelp("Renders an image of the blueprint string provided. Longer blueprints "
						+ "can be attached as files or linked with pastebin, hastebin, or gist URLs.")//
				//
				.create();

		bot.startAsync().awaitRunning();
	}
}
