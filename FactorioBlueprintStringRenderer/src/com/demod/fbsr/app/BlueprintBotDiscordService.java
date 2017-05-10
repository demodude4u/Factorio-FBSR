package com.demod.fbsr.app;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import com.demod.fbsr.Blueprint;
import com.demod.fbsr.BlueprintStringData;
import com.demod.fbsr.FBSR;
import com.google.common.util.concurrent.AbstractIdleService;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Message.Attachment;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class BlueprintBotDiscordService extends AbstractIdleService {

	private static final int ATTACHMENT_MAX_SIZE = 1024 * 1024;

	private static final String PREF_KEY_BOT_TOKEN = "bot_token";

	private static final Pattern blueprintPattern = Pattern.compile("([A-Za-z0-9+\\/]{90,})");
	private static final Pattern pastebinPattern = Pattern.compile("pastebin\\.com/([A-Za-z0-9]+)");

	public static void main(String[] args) {
		new BlueprintBotDiscordService().startAsync();
	}

	private JDA jda;

	private void findBlueprints(MessageReceivedEvent event, String content) {
		Matcher matcher = blueprintPattern.matcher(content);
		while (matcher.find()) {
			String blueprintString = matcher.group();
			try {
				event.getChannel().sendTyping().complete();

				BlueprintStringData blueprintStringData = new BlueprintStringData(blueprintString);
				if (blueprintStringData.getBlueprints().size() == 1) {
					BufferedImage image = FBSR.renderBlueprint(blueprintStringData.getBlueprints().get(0));
					try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
						ImageIO.write(image, "PNG", baos);
						byte[] imageData = baos.toByteArray();
						event.getChannel().sendFile(new ByteArrayInputStream(imageData), "blueprint.png", null)
								.complete();
					}

				} else {
					try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
							ZipOutputStream zos = new ZipOutputStream(baos)) {

						int counter = 1;
						for (Blueprint blueprint : blueprintStringData.getBlueprints()) {
							try {
								BufferedImage image = FBSR.renderBlueprint(blueprint);
								zos.putNextEntry(new ZipEntry("blueprint " + counter + ".png"));
								ImageIO.write(image, "PNG", zos);
							} catch (Exception e) {
								sendErrorToDemod(event, e, blueprintString);
								e.printStackTrace();
							}
							counter++;
						}

						zos.close();
						byte[] zipData = baos.toByteArray();
						event.getChannel()
								.sendFile(new ByteArrayInputStream(zipData), "blueprint book images.zip", null)
								.complete();
					}
				}
			} catch (Exception e) {
				sendErrorToDemod(event, e, blueprintString);
				e.printStackTrace();
			}
		}
	}

	private void findBlueprintsInURL(MessageReceivedEvent event, String url) {
		StringBuilder contentBuilder = new StringBuilder();
		try {
			URLConnection hc = new URL(url).openConnection();
			hc.setRequestProperty("User-Agent",
					"Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");

			try (BufferedReader br = new BufferedReader(new InputStreamReader(hc.getInputStream()))) {
				String line;
				while ((line = br.readLine()) != null) {
					contentBuilder.append(line).append('\n');
				}
				findBlueprints(event, contentBuilder.toString());
			}
		} catch (IOException e) {
			sendErrorToDemod(event, e, url);
			e.printStackTrace();
		}
	}

	private void findPastebins(MessageReceivedEvent event, String content) {
		Matcher matcher = pastebinPattern.matcher(content);
		while (matcher.find()) {
			String code = matcher.group(1);
			findBlueprintsInURL(event, "https://pastebin.com/raw/" + code);
		}
	}

	private void findTextAttachments(MessageReceivedEvent event) {
		for (Attachment attachment : event.getMessage().getAttachments()) {
			if (attachment.getSize() < ATTACHMENT_MAX_SIZE) {
				findBlueprintsInURL(event, attachment.getUrl());
			}
		}
	}

	private void sendErrorToDemod(MessageReceivedEvent event, Exception e, String context) {
		String message;
		if (event.getGuild() == null) {
			message = event.getAuthor().getName() + ": ```" + e.getMessage() + "```";
		} else {
			message = event.getGuild().getName() + " / #" + event.getChannel().getName() + " / "
					+ event.getAuthor().getName() + ": ```" + e.getMessage() + "```";
		}

		boolean contextFile = false;
		if (context != null) {
			if (message.length() + context.length() < 2000) {
				message += "\n" + context;
			} else {
				contextFile = true;
			}
		}

		PrivateChannel privateChannel = event.getJDA().getUserById("100075603016814592").openPrivateChannel()
				.complete();
		privateChannel.sendMessage(message).complete();
		if (contextFile) {
			privateChannel.sendFile(new ByteArrayInputStream(context.getBytes()), "context.txt", null).complete();
		}
	}

	@Override
	protected void shutDown() throws Exception {
		jda.shutdown(false);
	}

	@Override
	protected void startUp() throws Exception {
		Preferences preferences = Preferences.userNodeForPackage(getClass());
		String botToken = preferences.get(PREF_KEY_BOT_TOKEN, null);
		if (botToken == null) {
			botToken = JOptionPane.showInputDialog("Please enter your bot token:");
			if (botToken == null) {
				System.exit(0);
			}
		}

		jda = new JDABuilder(AccountType.BOT).setToken(botToken).addEventListener(new ListenerAdapter() {
			@Override
			public void onMessageReceived(MessageReceivedEvent event) {
				if (event.getAuthor().isBot()) {
					return;
				}

				// If not mentioned in a non-private channel
				if (!(event.getChannel() instanceof PrivateChannel) && !event.getMessage().getMentionedUsers().stream()
						.anyMatch(u -> u.getId().equals(event.getJDA().getSelfUser().getId()))) {
					return;
				}

				String content = event.getMessage().getStrippedContent();
				findBlueprints(event, content);
				findPastebins(event, content);
				findTextAttachments(event);
			}
		}).buildBlocking();

		preferences.put(PREF_KEY_BOT_TOKEN, botToken);
	}
}
