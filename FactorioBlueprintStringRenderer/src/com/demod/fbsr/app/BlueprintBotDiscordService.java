package com.demod.fbsr.app;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import com.demod.dcba.CommandHandler;
import com.demod.dcba.DCBA;
import com.demod.dcba.DiscordBot;
import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.Utils;
import com.demod.factorio.prototype.DataPrototype;
import com.demod.fbsr.Blueprint;
import com.demod.fbsr.BlueprintFinder;
import com.demod.fbsr.BlueprintReporting;
import com.demod.fbsr.BlueprintReporting.Level;
import com.demod.fbsr.BlueprintStringData;
import com.demod.fbsr.FBSR;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.WorldMap;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.util.concurrent.AbstractIdleService;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class BlueprintBotDiscordService extends AbstractIdleService {

	private static final String USERID_DEMOD = "100075603016814592";

	private static final Pattern debugPattern = Pattern.compile("DEBUG:([A-Za-z0-9_]+)");

	public static void main(String[] args) {
		new BlueprintBotDiscordService().startAsync();
	}

	private DiscordBot bot;

	private CommandHandler createPrototypeCommandHandler(Function<String, Optional<? extends DataPrototype>> query) {
		return event -> {
			String content = event.getMessage().getStrippedContent();

			String[] args = content.split("\\s");
			if (args.length < 2) {
				event.getChannel().sendMessage("You didn't specify a prototype name!").complete();
				return;
			}

			Optional<? extends DataPrototype> prototype = query.apply(args[1]);
			if (!prototype.isPresent()) {
				event.getChannel().sendMessage("I could not find the prototype for `" + args[1] + "`. :frowning:");
				return;
			}

			try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); PrintStream ps = new PrintStream(baos)) {
				ps.println("Lua Data Dump of " + prototype.get().getName() + " for Factorio " + FBSR.getVersion());
				ps.println();
				Utils.debugPrintLua(prototype.get().lua(), ps);
				ps.flush();
				event.getChannel().sendFile(baos.toByteArray(),
						prototype.get().getName() + "_dump_" + FBSR.getVersion() + ".txt", null).complete();
			} catch (IOException e) {
				e.printStackTrace();
			}
		};
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

	private void processBlueprints(List<BlueprintStringData> blueprintStrings, MessageReceivedEvent event,
			BlueprintReporting reporting, String content) {
		for (BlueprintStringData blueprintString : blueprintStrings) {
			try {
				event.getChannel().sendTyping().complete();

				System.out.println("Parsing blueprints: " + blueprintString.getBlueprints().size());
				if (blueprintString.getBlueprints().size() == 1) {
					BufferedImage image = FBSR.renderBlueprint(blueprintString.getBlueprints().get(0), reporting);
					byte[] imageData = generateDiscordFriendlyPNGImage(image);
					Message message = event.getChannel()
							.sendFile(new ByteArrayInputStream(imageData), "blueprint.png", null).complete();
					reporting.addImageURL(message.getAttachments().get(0).getUrl());
				} else {
					try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
							ZipOutputStream zos = new ZipOutputStream(baos)) {

						int counter = 1;
						for (Blueprint blueprint : blueprintString.getBlueprints()) {
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

	private void sendReportToDemod(MessageReceivedEvent event, BlueprintReporting reporting) {
		Optional<String> context = reporting.getContext();
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

		Level level = reporting.getLevel();
		if (level != Level.INFO) {
			builder.setColor(level.getColor());
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

		Multiset<String> uniqueWarnings = LinkedHashMultiset.create(warnings);
		if (!uniqueWarnings.isEmpty()) {
			builder.addField("Warnings",
					uniqueWarnings.entrySet().stream()
							.map(e -> e.getElement() + (e.getCount() > 1 ? " *(**" + e.getCount() + "** times)*" : ""))
							.collect(Collectors.joining("\n")),
					false);
		}

		Multiset<String> uniqueExceptions = LinkedHashMultiset.create();
		Optional<String> exceptionFile = Optional.empty();
		if (!exceptions.isEmpty()) {
			try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
				for (Exception e : exceptions) {
					if (uniqueExceptions.add(e.getClass().getSimpleName() + ": " + e.getMessage())) {
						e.printStackTrace();
						e.printStackTrace(pw);
					}
				}
				pw.flush();
				exceptionFile = Optional.of(sw.toString());
			} catch (IOException e1) {
				e1.printStackTrace();// XXX Uh... Houston, we have a problem...
			}
		}
		if (!uniqueExceptions.isEmpty()) {
			builder.addField("Exceptions",
					uniqueExceptions.entrySet().stream()
							.map(e -> e.getElement() + (e.getCount() > 1 ? " *(**" + e.getCount() + "** times)*" : ""))
							.collect(Collectors.joining("\n")),
					false);
		}

		PrivateChannel privateChannel = event.getJDA().getUserById(USERID_DEMOD).openPrivateChannel().complete();
		privateChannel.sendMessage(builder.build()).complete();

		if (context.isPresent()) {
			privateChannel.sendFile(new ByteArrayInputStream(context.get().getBytes()), "context.txt", null).complete();
		}
		if (exceptionFile.isPresent()) {
			privateChannel.sendFile(new ByteArrayInputStream(exceptionFile.get().getBytes()), "exceptions.txt", null)
					.complete();
		}
	}

	@Override
	protected void shutDown() throws Exception {
		bot.stopAsync().awaitTerminated();
	}

	@Override
	protected void startUp() throws Exception {
		DataTable table = FactorioData.getTable();
		System.out.println("Factorio " + FBSR.getVersion() + " Data Loaded.");

		bot = DCBA.builder()//
				.withCommandPrefix("-")
				//
				.setInfo("Blueprint Bot")//
				.withSupport("Find Demod and complain to him!")//
				.withTechnology("FBSR", "Factorio Blueprint String Renderer")//
				.withTechnology("FactorioDataWrapper", "Factorio Data Scraper")//
				.withCredits("Attribution", "Factorio - Made by Wube Software")//
				//
				.addCommand("blueprint", event -> {
					String content = event.getMessage().getStrippedContent();
					BlueprintReporting reporting = new BlueprintReporting();
					reporting.setContext(content);
					System.out.println("\n############################################################\n");
					findDebugOptions(reporting, content);
					processBlueprints(BlueprintFinder.search(content, reporting), event, reporting, content);
					sendReportToDemod(event, reporting);
				})//
				.withHelp("Renders an image of the blueprint string provided. Longer blueprints "
						+ "can be attached as files or linked with pastebin, hastebin, gitlab, or gist URLs.")//
				//
				.addCommand("prototypeEntity", createPrototypeCommandHandler(table::getEntity))//
				.withHelp("Provides a dump of the lua data for the specified entity prototype.")//
				.addCommand("prototypeRecipe", createPrototypeCommandHandler(table::getRecipe))//
				.withHelp("Provides a dump of the lua data for the specified recipe prototype.")//
				.addCommand("prototypeFluid", createPrototypeCommandHandler(table::getFluid))//
				.withHelp("Provides a dump of the lua data for the specified fluid prototype.")//
				.addCommand("prototypeItem", createPrototypeCommandHandler(table::getItem))//
				.withHelp("Provides a dump of the lua data for the specified item prototype.")//
				.addCommand("prototypeTechnology", createPrototypeCommandHandler(table::getTechnology))//
				.withHelp("Provides a dump of the lua data for the specified technology prototype.")//
				//
				.create();

		bot.startAsync().awaitRunning();
	}
}
