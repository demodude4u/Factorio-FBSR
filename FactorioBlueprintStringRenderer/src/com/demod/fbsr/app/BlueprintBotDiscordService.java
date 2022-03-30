package com.demod.fbsr.app;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.net.URL;
import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.luaj.vm2.LuaValue;

import com.demod.dcba.AutoCompleteHandler;
import com.demod.dcba.DCBA;
import com.demod.dcba.DiscordBot;
import com.demod.dcba.EventReply;
import com.demod.dcba.MessageCommandEvent;
import com.demod.dcba.SlashCommandEvent;
import com.demod.dcba.SlashCommandHandler;
import com.demod.factorio.Config;
import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.Utils;
import com.demod.factorio.prototype.DataPrototype;
import com.demod.fbsr.Blueprint;
import com.demod.fbsr.BlueprintEntity;
import com.demod.fbsr.BlueprintFinder;
import com.demod.fbsr.BlueprintStringData;
import com.demod.fbsr.FBSR;
import com.demod.fbsr.MapVersion;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.TaskReporting;
import com.demod.fbsr.TaskReporting.Level;
import com.demod.fbsr.WebUtils;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.app.WatchdogService.WatchdogReporter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.util.concurrent.AbstractIdleService;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.ChunkingFilter;

public class BlueprintBotDiscordService extends AbstractIdleService {

	private static final int MADEUP_NUMBER_FROM_AROUND_5_IN_THE_MORNING = 200;

	private static final Pattern debugPattern = Pattern.compile("DEBUG:([A-Za-z0-9_]+)");

	private static final Map<String, String> upgradeBeltsEntityMapping = new HashMap<>();
	static {
		upgradeBeltsEntityMapping.put("transport-belt", "fast-transport-belt");
		upgradeBeltsEntityMapping.put("underground-belt", "fast-underground-belt");
		upgradeBeltsEntityMapping.put("splitter", "fast-splitter");
		upgradeBeltsEntityMapping.put("fast-transport-belt", "express-transport-belt");
		upgradeBeltsEntityMapping.put("fast-underground-belt", "express-underground-belt");
		upgradeBeltsEntityMapping.put("fast-splitter", "express-splitter");
	}

	private DiscordBot bot;

	private JSONObject configJson;

	private String reportingUserID;
	private String reportingChannelID;
	private String hostingChannelID;

	private SlashCommandHandler createDataRawCommandHandler(Function<String[], Optional<LuaValue>> query) {
		return (event) -> {
			TaskReporting reporting = new TaskReporting();

			reporting.setContext(event.getCommandString());

			try {
				String key = event.getParamString("path");
				String[] path = key.split("\\.");
				Optional<LuaValue> lua = query.apply(path);
				if (!lua.isPresent()) {
					event.reply("I could not find a lua table for the path [`"
							+ Arrays.asList(path).stream().collect(Collectors.joining(", ")) + "`] :frowning:");
					return;
				}
				sendLuaDumpFile(event, "raw", key, lua.get(), reporting);
			} catch (Exception e) {
				reporting.addException(e);
			}
			sendReport(event, reporting);
		};
	}

	private MessageEmbed createExceptionReportEmbed(String author, String authorURL, TaskReporting reporting)
			throws IOException {
		List<Exception> exceptions = reporting.getExceptions();
		List<String> warnings = reporting.getWarnings();

		EmbedBuilder builder = new EmbedBuilder();
		builder.setAuthor(author, null, authorURL);
		builder.setTimestamp(Instant.now());

		Level level = reporting.getLevel();
		if (level != Level.INFO) {
			builder.setColor(level.getColor());
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
			}
		}
		if (!uniqueExceptions.isEmpty()) {
			builder.addField("Exceptions",
					uniqueExceptions.entrySet().stream()
							.map(e -> e.getElement() + (e.getCount() > 1 ? " *(**" + e.getCount() + "** times)*" : ""))
							.collect(Collectors.joining("\n")),
					false);
		}
		if (exceptionFile.isPresent()) {
			builder.addField("Stack Trace(s)",
					WebUtils.uploadToHostingService("exceptions.txt", exceptionFile.get().getBytes()).toString(),
					false);
		}

		return builder.build();
	}

	private AutoCompleteHandler createPrototypeAutoCompleteHandler(Map<String, ? extends DataPrototype> map) {
		return (event) -> {
			String name = event.getParamString("name").trim().toLowerCase();

			if (name.isEmpty()) {
				event.reply(ImmutableList.of());
				return;
			}

			List<String> nameStartsWith = new ArrayList<>();
			List<String> nameContains = new ArrayList<>();
			map.keySet().stream().sorted().forEach(n -> {
				String lowerCase = n.toLowerCase();
				if (lowerCase.startsWith(name)) {
					nameStartsWith.add(n);
				} else if (lowerCase.contains(name)) {
					nameContains.add(n);
				}
			});

			List<String> choices = ImmutableList.<String>builder().addAll(nameStartsWith).addAll(nameContains).build()
					.stream().limit(OptionData.MAX_CHOICES).collect(Collectors.toList());
			event.reply(choices);
		};
	}

	private SlashCommandHandler createPrototypeCommandHandler(String category,
			Map<String, ? extends DataPrototype> map) {
		return (event) -> {
			TaskReporting reporting = new TaskReporting();
			reporting.setContext(event.getCommandString());

			try {
				String search = event.getParamString("name");
				Optional<? extends DataPrototype> prototype = Optional.ofNullable(map.get(search));
				if (!prototype.isPresent()) {
					LevenshteinDistance levenshteinDistance = LevenshteinDistance.getDefaultInstance();
					List<String> suggestions = map.keySet().stream()
							.map(k -> new SimpleEntry<>(k, levenshteinDistance.apply(search, k)))
							.sorted((p1, p2) -> Integer.compare(p1.getValue(), p2.getValue())).limit(5)
							.map(p -> p.getKey()).collect(Collectors.toList());
					event.reply("I could not find the " + category + " prototype for `" + search
							+ "`. :frowning:\nDid you mean:\n"
							+ suggestions.stream().map(s -> "\t - " + s).collect(Collectors.joining("\n")));
					return;
				}

				sendLuaDumpFile(event, category, prototype.get().getName(), prototype.get().lua(), reporting);
			} catch (Exception e) {
				reporting.addException(e);
			}
			sendReport(event, reporting);
		};
	}

	private MessageEmbed createReportEmbed(String author, String authorURL, TaskReporting reporting)
			throws IOException {
		Optional<String> context = reporting.getContext();
		List<Exception> exceptions = reporting.getExceptions();
		List<String> warnings = reporting.getWarnings();
		List<Entry<Optional<String>, String>> images = reporting.getImages();
		List<String> links = reporting.getLinks();
		List<String> downloads = reporting.getDownloads();
		Set<String> info = reporting.getInfo();
		Optional<Message> contextMessage = reporting.getContextMessage();
		List<Long> renderTimes = reporting.getRenderTimes();

		EmbedBuilder builder = new EmbedBuilder();
		builder.setAuthor(author, null, authorURL);
		builder.setTimestamp(Instant.now());

		Level level = reporting.getLevel();
		if (level != Level.INFO) {
			builder.setColor(level.getColor());
		}

		if (context.isPresent() && context.get().length() <= MADEUP_NUMBER_FROM_AROUND_5_IN_THE_MORNING) {
			builder.addField("Context", context.get(), false);
		} else if (context.isPresent()) {
			builder.addField("Context Link",
					WebUtils.uploadToHostingService("context.txt", context.get().getBytes()).toString(), false);
		}

		if (contextMessage.isPresent()) {
			builder.addField("Context Message", "[Message Link](" + contextMessage.get().getJumpUrl() + ")", false);
		}

		if (!links.isEmpty()) {
			builder.addField("Link(s)", links.stream().collect(Collectors.joining("\n")), false);
		}

		if (!images.isEmpty()) {
			try {
				builder.setImage(images.get(0).getValue());
			} catch (IllegalArgumentException e) {
				// Local Storage Image, can't preview!
			}
		}
		if (images.size() > 1) {
			WebUtils.addPossiblyLargeEmbedField(builder, "Additional Image(s)",
					images.stream().skip(1).map(Entry::getValue).collect(Collectors.joining("\n")), false);
		}

		if (!downloads.isEmpty()) {
			builder.addField("Download(s)", downloads.stream().collect(Collectors.joining("\n")), false);
		}

		if (!info.isEmpty()) {
			builder.addField("Info", info.stream().collect(Collectors.joining("\n")), false);
		}

		if (!renderTimes.isEmpty()) {
			builder.addField("Render Time", renderTimes.stream().mapToLong(l -> l).sum() + " ms"
					+ (renderTimes.size() > 1
							? (" [" + renderTimes.stream().map(Object::toString).collect(Collectors.joining(", "))
									+ "]")
							: ""),
					false);
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
			}
		}
		if (!uniqueExceptions.isEmpty()) {
			builder.addField("Exceptions",
					uniqueExceptions.entrySet().stream()
							.map(e -> e.getElement() + (e.getCount() > 1 ? " *(**" + e.getCount() + "** times)*" : ""))
							.collect(Collectors.joining("\n")),
					false);
		}
		if (exceptionFile.isPresent()) {
			builder.addField("Stack Trace(s)",
					WebUtils.uploadToHostingService("exceptions.txt", exceptionFile.get().getBytes()).toString(),
					false);
		}

		return builder.build();
	}

	private void findDebugOptions(TaskReporting reporting, String content) {
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

	private byte[] generateDiscordFriendlyPNGImage(BufferedImage image) {
		byte[] imageData = WebUtils.getImageData(image);
		if (imageData.length > 8000000) {
			return generateDiscordFriendlyPNGImage(
					RenderUtils.scaleImage(image, image.getWidth() / 2, image.getHeight() / 2));
		}
		return imageData;
	}

	private String getReadableAddress(MessageCommandEvent event) {
		if (event.isFromType(ChannelType.PRIVATE)) {// event.getGuild() == null) {
			return event.getUser().getName();
		} else {
			return event.getGuild().getName() + " / #" + event.getMessageChannel().getName() + " / "
					+ event.getUser().getName();
		}
	}

	private String getReadableAddress(SlashCommandEvent event) {
		if (event.isFromType(ChannelType.PRIVATE)) {// event.getGuild() == null) {
			return event.getUser().getName();
		} else {
			return event.getGuild().getName() + " / #" + event.getMessageChannel().getName() + " / "
					+ event.getUser().getName();
		}
	}

	private void handleBlueprintBookAssembleCommand(SlashCommandEvent event) {
		String content = event.getCommandString();

		Optional<Attachment> attachment = event.optParamAttachment("file");
		if (attachment.isPresent()) {
			content += " " + attachment.get().getUrl();
		}

		TaskReporting reporting = new TaskReporting();
		reporting.setContext(content);

		List<BlueprintStringData> blueprintStrings = BlueprintFinder.search(content, reporting);

//		if (event instanceof MessageCommandEvent) {
//			MessageCommandEvent messageEvent = (MessageCommandEvent) event;
//			if (!messageEvent.getMessage().getAttachments().isEmpty()) {
//				String url = messageEvent.getMessage().getAttachments().get(0).getUrl();
//				reporting.addLink(url);
//				blueprintStrings.addAll(BlueprintFinder.search(url, reporting));
//			}
//		}

		if (!blueprintStrings.isEmpty()) {
			List<Blueprint> blueprints = blueprintStrings.stream().flatMap(bs -> bs.getBlueprints().stream())
					.collect(Collectors.toList());

			JSONObject json = new JSONObject();
			Utils.terribleHackToHaveOrderedJSONObject(json);
			JSONObject bookJson = new JSONObject();
			Utils.terribleHackToHaveOrderedJSONObject(bookJson);
			json.put("blueprint_book", bookJson);
			JSONArray blueprintsJson = new JSONArray();
			bookJson.put("blueprints", blueprintsJson);
			bookJson.put("item", "blueprint-book");
			bookJson.put("active_index", 0);

			MapVersion latestVersion = new MapVersion();
			int index = 0;
			for (Blueprint blueprint : blueprints) {
				blueprint.json().put("index", index);

				latestVersion = MapVersion.max(latestVersion, blueprint.getVersion());

				blueprintsJson.put(blueprint.json());

				index++;
			}

			String bookLabel = blueprintStrings.stream().filter(BlueprintStringData::isBook)
					.map(BlueprintStringData::getLabel).filter(Optional::isPresent).map(Optional::get).map(String::trim)
					.distinct().collect(Collectors.joining(" & "));
			if (!bookLabel.isEmpty()) {
				bookJson.put("label", bookLabel);
			}

			if (!latestVersion.isEmpty()) {
				bookJson.put("version", latestVersion.getSerialized());
			}

			try {
				reporting.addInfo("Assembled Book: " + WebUtils.uploadToHostingService("blueprintBook.txt",
						(" " + BlueprintStringData.encode(json)).getBytes()));
			} catch (Exception e) {
				reporting.addException(e);
			}

		} else {
			reporting.addInfo("No blueprint found!");
		}

		if (!reporting.getInfo().isEmpty()) {
			event.reply(reporting.getInfo().stream().collect(Collectors.joining("\n")));
		}

		sendReport(event, reporting);
	}

	private void handleBlueprintBookExtractCommand(SlashCommandEvent event) {
		String content = event.getCommandString();

		Optional<Attachment> attachment = event.optParamAttachment("file");
		if (attachment.isPresent()) {
			content += " " + attachment.get().getUrl();
		}

		TaskReporting reporting = new TaskReporting();
		reporting.setContext(content);

		List<BlueprintStringData> blueprintStrings = BlueprintFinder.search(content, reporting);

//		if (event instanceof MessageCommandEvent) {
//			MessageCommandEvent messageEvent = (MessageCommandEvent) event;
//			if (!messageEvent.getMessage().getAttachments().isEmpty()) {
//				String url = messageEvent.getMessage().getAttachments().get(0).getUrl();
//				reporting.addLink(url);
//				blueprintStrings.addAll(BlueprintFinder.search(url, reporting));
//			}
//		}

		List<Blueprint> blueprints = blueprintStrings.stream().flatMap(bs -> bs.getBlueprints().stream())
				.collect(Collectors.toList());
		List<Entry<URL, String>> links = new ArrayList<>();
		for (Blueprint blueprint : blueprints) {
			try {
				blueprint.json().remove("index");

				URL url = WebUtils.uploadToHostingService("blueprint.txt",
						(/*
							 * blueprint.getLabel().orElse("Blueprint String") + ": "
							 */" " + BlueprintStringData.encode(blueprint.json())).getBytes());
				links.add(new SimpleEntry<>(url, blueprint.getLabel().orElse(null)));
			} catch (Exception e) {
				reporting.addException(e);
			}
		}

		List<EmbedBuilder> embedBuilders = new ArrayList<>();

		if (!links.isEmpty()) {
			ArrayDeque<String> lines = links.stream()
					.map(p -> (p.getValue() != null && !p.getValue().isEmpty())
							? ("[" + p.getValue() + "](" + p.getKey() + ")")
							: p.getKey().toString())
					.collect(Collectors.toCollection(ArrayDeque::new));
			while (!lines.isEmpty()) {
				EmbedBuilder builder = new EmbedBuilder();
				StringBuilder description = new StringBuilder();
				while (!lines.isEmpty()) {
					if (description.length() + lines.peek().length() + 1 < MessageEmbed.DESCRIPTION_MAX_LENGTH) {
						description.append(lines.pop()).append('\n');
					} else {
						break;
					}
				}
				builder.setDescription(description);
				embedBuilders.add(builder);
			}

		} else {
			embedBuilders.add(new EmbedBuilder().setDescription("Blueprint not found!"));
		}

		if (!reporting.getInfo().isEmpty()) {
			embedBuilders.get(embedBuilders.size() - 1)
					.setFooter(reporting.getInfo().stream().collect(Collectors.joining("\n")));
		}

		List<MessageEmbed> embeds = embedBuilders.stream().map(EmbedBuilder::build).collect(Collectors.toList());
		for (MessageEmbed embed : embeds) {
			Message message = event.replyEmbed(embed);
			reporting.setContextMessage(message);
		}

		sendReport(event, reporting);
	}

	private void handleBlueprintItemsCommand(SlashCommandEvent event) {
		DataTable table;
		try {
			table = FactorioData.getTable();
		} catch (JSONException | IOException e1) {
			throw new InternalError(e1);
		}

		String content = event.getCommandString();

		Optional<Attachment> attachment = event.optParamAttachment("file");
		if (attachment.isPresent()) {
			content += " " + attachment.get().getUrl();
		}

		TaskReporting reporting = new TaskReporting();
		reporting.setContext(content);

		List<BlueprintStringData> blueprintStringDatas;

//		if (event instanceof MessageCommandEvent
//				&& !((MessageCommandEvent) event).getMessage().getAttachments().isEmpty()) {
//			String url = ((MessageCommandEvent) event).getMessage().getAttachments().get(0).getUrl();
//			reporting.addLink(url);
//			blueprintStringDatas = BlueprintFinder.search(url, reporting);
//		} else {
		blueprintStringDatas = BlueprintFinder.search(content, reporting);
//		}

		Map<String, Double> totalItems = new LinkedHashMap<>();
		for (BlueprintStringData blueprintStringData : blueprintStringDatas) {
			for (Blueprint blueprint : blueprintStringData.getBlueprints()) {
				Map<String, Double> items = FBSR.generateTotalItems(table, blueprint, reporting);
				items.forEach((k, v) -> {
					totalItems.compute(k, ($, old) -> old == null ? v : old + v);
				});
			}
		}

		if (!totalItems.isEmpty()) {
			try {
				String responseContent = totalItems.entrySet().stream()
						.sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
						.map(e -> table.getWikiItemName(e.getKey()) + ": " + RenderUtils.fmtDouble2(e.getValue()))
						.collect(Collectors.joining("\n"));
				String responseContentUrl = WebUtils.uploadToHostingService("items.txt", responseContent.getBytes())
						.toString();
				reporting.addLink(responseContentUrl);

				String response = "```ldif\n" + responseContent + "```";
				if (response.length() < 2000) {
					event.reply(response);
				} else {
					reporting.addInfo(responseContentUrl);
				}
			} catch (IOException e) {
				reporting.addException(e);
			}
		} else {
			if (reporting.getBlueprintStrings().isEmpty()) {
				reporting.addInfo("No blueprint found!");
			} else {
				reporting.addInfo("I couldn't find any items!");
			}
		}

		if (!reporting.getInfo().isEmpty()) {
			event.reply(reporting.getInfo().stream().collect(Collectors.joining("\n")));
		}

		sendReport(event, reporting);
	}

	private void handleBlueprintItemsRawCommand(SlashCommandEvent event) {
		DataTable table;
		try {
			table = FactorioData.getTable();
		} catch (JSONException | IOException e1) {
			throw new InternalError(e1);
		}

		String content = event.getCommandString();

		Optional<Attachment> attachment = event.optParamAttachment("file");
		if (attachment.isPresent()) {
			content += " " + attachment.get().getUrl();
		}

		TaskReporting reporting = new TaskReporting();
		reporting.setContext(content);

		List<BlueprintStringData> blueprintStringDatas;

//		if (event instanceof MessageCommandEvent
//				&& !((MessageCommandEvent) event).getMessage().getAttachments().isEmpty()) {
//			String url = ((MessageCommandEvent) event).getMessage().getAttachments().get(0).getUrl();
//			reporting.addLink(url);
//			blueprintStringDatas = BlueprintFinder.search(url, reporting);
//		} else {
		blueprintStringDatas = BlueprintFinder.search(content, reporting);
//		}

		Map<String, Double> totalItems = new LinkedHashMap<>();
		for (BlueprintStringData blueprintStringData : blueprintStringDatas) {
			for (Blueprint blueprint : blueprintStringData.getBlueprints()) {
				Map<String, Double> items = FBSR.generateTotalItems(table, blueprint, reporting);
				items.forEach((k, v) -> {
					totalItems.compute(k, ($, old) -> old == null ? v : old + v);
				});
			}
		}

		Map<String, Double> rawItems = FBSR.generateTotalRawItems(table, table.getRecipes(), totalItems);

		if (!rawItems.isEmpty()) {
			try {
				String responseContent = rawItems.entrySet().stream()
						.sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
						.map(e -> table.getWikiItemName(e.getKey()) + ": " + RenderUtils.fmtDouble2(e.getValue()))
						.collect(Collectors.joining("\n"));
				String responseContentUrl = WebUtils.uploadToHostingService("raw-items.txt", responseContent.getBytes())
						.toString();
				reporting.addLink(responseContentUrl);

				String response = "```ldif\n" + responseContent + "```";
				if (response.length() < 2000) {
					event.reply(response);
				} else {
					reporting.addInfo(responseContentUrl);
				}
			} catch (IOException e) {
				reporting.addException(e);
			}
		} else {
			if (reporting.getBlueprintStrings().isEmpty()) {
				reporting.addInfo("No blueprint found!");
			} else {
				reporting.addInfo("I couldn't find any raw items!");
			}
		}

		if (!reporting.getInfo().isEmpty()) {
			event.reply(reporting.getInfo().stream().collect(Collectors.joining("\n")));
		}

		sendReport(event, reporting);
	}

	private void handleBlueprintJsonCommand(SlashCommandEvent event) {
		String content = event.getCommandString();

		Optional<Attachment> attachment = event.optParamAttachment("file");
		if (attachment.isPresent()) {
			content += " " + attachment.get().getUrl();
		}

		TaskReporting reporting = new TaskReporting();
		reporting.setContext(content);
		List<String> results = BlueprintFinder.searchRaw(content, reporting);
		if (!results.isEmpty()) {
			try {
				byte[] bytes = BlueprintStringData.decode(results.get(0)).toString(2).getBytes();
				if (results.size() == 1) {
					URL url = WebUtils.uploadToHostingService("blueprint.json", bytes);
					event.reply("Blueprint JSON: " + url.toString());
					reporting.addLink(url.toString());
				} else {
					try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
							ZipOutputStream zos = new ZipOutputStream(baos)) {
						for (int i = 0; i < results.size(); i++) {
							try {
								String blueprintString = results.get(i);
								zos.putNextEntry(new ZipEntry("blueprint " + (i + 1) + ".json"));
								zos.write(BlueprintStringData.decode(blueprintString).toString(2).getBytes());
							} catch (Exception e) {
								reporting.addException(e);
							}
						}
						zos.close();
						byte[] zipData = baos.toByteArray();
						try {
							Message response = event.replyFile(zipData, "blueprint JSON files.zip");
							reporting.addDownload(response.getAttachments().get(0).getUrl());
						} catch (Exception e) {
							reporting.addInfo("Blueprint JSON Files: "
									+ WebUtils.uploadToHostingService("blueprint JSON files.zip", zipData));
						}
					} catch (IOException e) {
						reporting.addException(e);
					}
				}
			} catch (Exception e) {
				reporting.addException(e);
			}
		} else {
			reporting.addInfo("No blueprint found!");
		}

		sendReport(event, reporting);
	}

	private void handleBlueprintMessageCommand(MessageCommandEvent event) {
		String content = event.getMessage().getContentDisplay();

		for (Attachment attachment : event.getMessage().getAttachments()) {
			content += " " + attachment.getUrl();
		}

		TaskReporting reporting = new TaskReporting();
		reporting.setContext(content);

		findDebugOptions(reporting, content);

		List<EmbedBuilder> embedBuilders = processBlueprints(BlueprintFinder.search(content, reporting), reporting,
				new JSONObject());

		EmbedBuilder firstEmbed = embedBuilders.get(0);
//		if (firstEmbed.getDescriptionBuilder().length() > 0) {
//			firstEmbed.addField(
//					new MessageEmbed.Field("", "[Blueprint String](" + event.getMessage().getJumpUrl() + ")", false));
//		} else {
//			firstEmbed.setDescription("[Blueprint String](" + event.getMessage().getJumpUrl() + ")");
//		}
		firstEmbed.setAuthor(event.getMessage().getAuthor().getName(), event.getMessage().getJumpUrl(),
				event.getMessage().getAuthor().getEffectiveAvatarUrl());

//		embedBuilders.get(0).addField("", "[Blueprint String](" + event.getMessage().getJumpUrl() + ")", false);

		if (!reporting.getInfo().isEmpty()) {
			embedBuilders.get(embedBuilders.size() - 1)
					.setFooter(reporting.getInfo().stream().collect(Collectors.joining("\n")));
		}

		List<MessageEmbed> embeds = embedBuilders.stream().map(EmbedBuilder::build).collect(Collectors.toList());
		for (MessageEmbed embed : embeds) {
			Message message = event.replyEmbed(embed);
			reporting.setContextMessage(message);
		}

		sendReport(event, reporting);
	}

	private void handleBlueprintSlashCommand(SlashCommandEvent event) {
		String content = event.getCommandString();

		Optional<Attachment> attachment = event.optParamAttachment("file");
		if (attachment.isPresent()) {
			content += " " + attachment.get().getUrl();
		}

		TaskReporting reporting = new TaskReporting();
		reporting.setContext(content);

		findDebugOptions(reporting, content);

		JSONObject options = new JSONObject();
		event.optParamBoolean("simple").ifPresent(b -> options.put("show-info-panels", !b));
		event.optParamLong("max-width").ifPresent(l -> options.put("max-width", l.intValue()));
		event.optParamLong("max-height").ifPresent(l -> options.put("max-height", l.intValue()));

		List<EmbedBuilder> embedBuilders = processBlueprints(BlueprintFinder.search(content, reporting), reporting,
				options);

		if (!reporting.getInfo().isEmpty()) {
			embedBuilders.get(embedBuilders.size() - 1)
					.setFooter(reporting.getInfo().stream().collect(Collectors.joining("\n")));
		}

		List<MessageEmbed> embeds = embedBuilders.stream().map(EmbedBuilder::build).collect(Collectors.toList());
		for (MessageEmbed embed : embeds) {
			Message message = event.replyEmbed(embed);
			reporting.setContextMessage(message);
		}

		sendReport(event, reporting);
	}

	private void handleBlueprintTotalsCommand(SlashCommandEvent event) {
		DataTable table;
		try {
			table = FactorioData.getTable();
		} catch (JSONException | IOException e1) {
			throw new InternalError(e1);
		}

		String content = event.getCommandString();

		Optional<Attachment> attachment = event.optParamAttachment("file");
		if (attachment.isPresent()) {
			content += " " + attachment.get().getUrl();
		}

		TaskReporting reporting = new TaskReporting();
		reporting.setContext(content);

		List<BlueprintStringData> blueprintStringDatas;

//		if (event instanceof MessageCommandEvent
//				&& !((MessageCommandEvent) event).getMessage().getAttachments().isEmpty()) {
//			String url = ((MessageCommandEvent) event).getMessage().getAttachments().get(0).getUrl();
//			reporting.addLink(url);
//			blueprintStringDatas = BlueprintFinder.search(url, reporting);
//		} else {
		blueprintStringDatas = BlueprintFinder.search(content, reporting);
//		}

		Map<String, Double> totalItems = new LinkedHashMap<>();
		for (BlueprintStringData blueprintStringData : blueprintStringDatas) {
			for (Blueprint blueprint : blueprintStringData.getBlueprints()) {
				Map<String, Double> items = FBSR.generateSummedTotalItems(table, blueprint, reporting);
				items.forEach((k, v) -> {
					totalItems.compute(k, ($, old) -> old == null ? v : old + v);
				});
			}
		}

		if (!totalItems.isEmpty()) {
			try {
				String responseContent = totalItems.entrySet().stream()
						.sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
						.map(e -> e.getKey() + ": " + RenderUtils.fmtDouble2(e.getValue()))
						.collect(Collectors.joining("\n"));
				String responseContentUrl = WebUtils.uploadToHostingService("items.txt", responseContent.getBytes())
						.toString();
				reporting.addLink(responseContentUrl);

				String response = "```ldif\n" + responseContent + "```";
				if (response.length() < 2000) {
					event.reply(response);
				} else {
					reporting.addInfo(responseContentUrl);
				}
			} catch (IOException e) {
				reporting.addException(e);
			}
		} else {
			if (reporting.getBlueprintStrings().isEmpty()) {
				reporting.addInfo("No blueprint found!");
			} else {
				reporting.addInfo("I couldn't find any entities, tiles or modules!");
			}
		}

		if (!reporting.getInfo().isEmpty()) {
			event.reply(reporting.getInfo().stream().collect(Collectors.joining("\n")));
		}

		sendReport(event, reporting);
	}

	private void handleBlueprintUpgradeBeltsCommand(SlashCommandEvent event) {
		String content = event.getCommandString();

		Optional<Attachment> attachment = event.optParamAttachment("file");
		if (attachment.isPresent()) {
			content += " " + attachment.get().getUrl();
		}

		TaskReporting reporting = new TaskReporting();
		reporting.setContext(content);

		List<BlueprintStringData> blueprintStringDatas;

//		if (event instanceof MessageCommandEvent
//				&& !((MessageCommandEvent) event).getMessage().getAttachments().isEmpty()) {
//			String url = ((MessageCommandEvent) event).getMessage().getAttachments().get(0).getUrl();
//			reporting.addLink(url);
//			blueprintStringDatas = BlueprintFinder.search(url, reporting);
//		} else {
		blueprintStringDatas = BlueprintFinder.search(content, reporting);
//		}

		int upgradedCount = 0;
		for (BlueprintStringData blueprintStringData : blueprintStringDatas) {
			for (Blueprint blueprint : blueprintStringData.getBlueprints()) {
				for (BlueprintEntity blueprintEntity : blueprint.getEntities()) {
					String upgradeName = upgradeBeltsEntityMapping.get(blueprintEntity.getName());
					if (upgradeName != null) {
						blueprintEntity.json().put("name", upgradeName);
						upgradedCount++;
					}
				}
			}
		}

		if (upgradedCount > 0) {
			reporting.addInfo("Upgraded " + upgradedCount + " entities.");
			for (BlueprintStringData blueprintStringData : blueprintStringDatas) {
				try {
					reporting
							.addInfo(WebUtils
									.uploadToHostingService("blueprint.txt",
											BlueprintStringData.encode(blueprintStringData.json()).getBytes())
									.toString());
				} catch (IOException e) {
					reporting.addException(e);
				}
			}
		} else {
			if (reporting.getBlueprintStrings().isEmpty()) {
				reporting.addInfo("No blueprint found!");
			} else {
				reporting.addInfo("I couldn't find anything to upgrade!");
			}
		}

		if (!reporting.getInfo().isEmpty()) {
			event.reply(reporting.getInfo().stream().collect(Collectors.joining("\n")));
		}

		sendReport(event, reporting);
	}

	@SuppressWarnings("unused")
	private void handleRedditCheckThingsCommand(SlashCommandEvent event) {
		String content = event.getCommandString();
		TaskReporting reporting = new TaskReporting();
		reporting.setContext(content);

		try {
			ServiceFinder.findService(BlueprintBotRedditService.class).ifPresent(s -> {
				try {
					s.processRequest(event.getParamString("id"));
					reporting.addInfo("Request successful!");
				} catch (Exception e) {
					reporting.addException(e);
				}
			});
		} catch (Exception e) {
			reporting.addException(e);
		}
		sendReport(event, reporting);
	}

	private List<EmbedBuilder> processBlueprints(List<BlueprintStringData> blueprintStrings, TaskReporting reporting,
			JSONObject options) {

		List<EmbedBuilder> embedBuilders = new ArrayList<>();

		List<Entry<Optional<String>, BufferedImage>> images = new ArrayList<>();

		for (BlueprintStringData blueprintString : blueprintStrings) {
			System.out.println("Parsing blueprints: " + blueprintString.getBlueprints().size());
			for (Blueprint blueprint : blueprintString.getBlueprints()) {
				try {
					BufferedImage image = FBSR.renderBlueprint(blueprint, reporting, options);
					images.add(new SimpleEntry<>(blueprint.getLabel(), image));
				} catch (Exception e) {
					reporting.addException(e);
				}
			}
		}

		try {
			if (images.size() == 0) {
				embedBuilders.add(new EmbedBuilder().setDescription("Blueprint not found!"));

			} else if (images.size() == 1) {
				Entry<Optional<String>, BufferedImage> entry = images.get(0);
				BufferedImage image = entry.getValue();
				URL url = WebUtils.uploadToHostingService("blueprint.png", image);
				EmbedBuilder builder = new EmbedBuilder();
				builder.setImage(url.toString());
				embedBuilders.add(builder);
				reporting.addImage(entry.getKey(), url.toString());

			} else {
				List<Entry<URL, String>> links = new ArrayList<>();
				for (Entry<Optional<String>, BufferedImage> entry : images) {
					BufferedImage image = entry.getValue();
					links.add(new SimpleEntry<>(WebUtils.uploadToHostingService("blueprint.png", image),
							entry.getKey().orElse("")));
				}

				ArrayDeque<String> lines = new ArrayDeque<>();
				for (int i = 0; i < links.size(); i++) {
					Entry<URL, String> entry = links.get(i);
					if (entry.getValue() != null && !entry.getValue().trim().isEmpty()) {
						lines.add("[" + entry.getValue().trim() + "](" + entry.getKey() + ")");
					} else {
						lines.add("[Blueprint " + (i + 1) + "](" + entry.getKey() + ")");
					}
				}

				while (!lines.isEmpty()) {
					EmbedBuilder builder = new EmbedBuilder();
					StringBuilder description = new StringBuilder();
					while (!lines.isEmpty()) {
						if (description.length() + lines.peek().length() + 1 < MessageEmbed.DESCRIPTION_MAX_LENGTH) {
							description.append(lines.pop()).append('\n');
						} else {
							break;
						}
					}
					builder.setDescription(description);
					embedBuilders.add(builder);
				}
			}
		} catch (Exception e) {
			reporting.addException(e);
		}

		return embedBuilders;
	}

	private void sendLuaDumpFile(EventReply event, String category, String name, LuaValue lua, TaskReporting reporting)
			throws IOException {
		JSONObject json = new JSONObject();
		Utils.terribleHackToHaveOrderedJSONObject(json);
		json.put("name", name);
		json.put("category", category);
		json.put("version", FBSR.getVersion());
		json.put("data", Utils.<JSONObject>convertLuaToJson(lua));
		String fileName = category + "_" + name + "_dump_" + FBSR.getVersion() + ".json";
		URL url = WebUtils.uploadToHostingService(fileName, json.toString(2).getBytes());
		event.reply(category + " " + name + " lua dump: [" + fileName + "](" + url.toString() + ")");
		reporting.addLink(url.toString());
	}

	public void sendReport(MessageCommandEvent event, TaskReporting reporting) {
		if (!reporting.getExceptions().isEmpty()) {
			event.replyEmbed(
					new EmbedBuilder().appendDescription("There was a problem completing your request.").build());
		}

		sendReport(getReadableAddress(event), event.getUser().getEffectiveAvatarUrl(), reporting);
	}

	public void sendReport(SlashCommandEvent event, TaskReporting reporting) {
		if (!reporting.getExceptions().isEmpty()) {
			event.replyEmbed(
					new EmbedBuilder().appendDescription("There was a problem completing your request.").build());
		}

		sendReport(getReadableAddress(event), event.getUser().getEffectiveAvatarUrl(), reporting);
	}

	public void sendReport(String author, String authorURL, TaskReporting reporting) {
		try {
			PrivateChannel privateChannel = bot.getJDA().openPrivateChannelById(reportingUserID).complete();
			privateChannel.sendMessageEmbeds(createReportEmbed(author, authorURL, reporting)).complete();
			if (!reporting.getExceptions().isEmpty()) {
				TextChannel textChannel = bot.getJDA().getTextChannelById(reportingChannelID);
				if (textChannel != null) {
					textChannel.sendMessageEmbeds(createExceptionReportEmbed(author, authorURL, reporting)).complete();
				}
			}

		} catch (Exception e) {
			PrivateChannel privateChannel = bot.getJDA().openPrivateChannelById(reportingUserID).complete();
			privateChannel.sendMessage("Failed to create report!").complete();
			try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
				e.printStackTrace();
				e.printStackTrace(pw);
				pw.flush();
				privateChannel.sendFile(sw.toString().getBytes(), "Exception.txt").complete();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	@Override
	protected void shutDown() {
		ServiceFinder.removeService(this);
		ServiceFinder.removeService(WatchdogReporter.class);
		bot.stopAsync().awaitTerminated();
	}

	@Override
	protected void startUp() throws JSONException, IOException {
		configJson = Config.get().getJSONObject("discord");

		DataTable table = FactorioData.getTable();
		System.out.println("Factorio " + FBSR.getVersion() + " Data Loaded.");

		bot = DCBA.builder()//
				.setInfo("Blueprint Bot")//
				.withSupport(
						"Find Demod and complain to him!\nYou can find him over in the [Factorio Discord.](https://discord.gg/factorio)")//
				.withTechnology("[FBSR](https://github.com/demodude4u/Factorio-FBSR)",
						"Factorio Blueprint String Renderer")//
				.withTechnology("[FactorioDataWrapper](https://github.com/demodude4u/Java-Factorio-Data-Wrapper)",
						"Factorio Data Scraper")//
				.withCredits("Attribution", "[Factorio](https://www.factorio.com/) - Made by Wube Software")//
				.withCredits("Contributors", "Demod")//
				.withCredits("Contributors", "Bilka")//
				.withCredits("Contributors", "FactorioBlueprints")//
				.withCredits("Contributors", "acid")//
				.withCredits("Contributors", "Vilsol")//
				.withInvite(new Permission[] { //
						Permission.VIEW_CHANNEL, //
						Permission.MESSAGE_SEND, //
						Permission.MESSAGE_ATTACH_FILES, //
						Permission.MESSAGE_EXT_EMOJI, //
						Permission.MESSAGE_EMBED_LINKS, //
						Permission.MESSAGE_HISTORY, //
						Permission.MESSAGE_ADD_REACTION,//
				})//
				.withCustomField("Need old !blueprint commands?",
						"[BlueprintBot Legacy Invite Link](https://discord.com/oauth2/authorize?scope=bot&client_id=958469202824552498&permissions=379968)")//
				//
				.addMessageCommand("Blueprint Image", event -> handleBlueprintMessageCommand(event))
				//
				.addSlashCommand("blueprint/string", "Renders an image of the blueprint string.",
						event -> handleBlueprintSlashCommand(event))//
				.withParam(OptionType.STRING, "string", "Blueprint string.")//
				.withOptionalParam(OptionType.BOOLEAN, "simple", "Set True to show just the image, no side panels.")
				.withOptionalParam(OptionType.INTEGER, "max-width", "Maximum width of image, in pixels.")
				.withOptionalParam(OptionType.INTEGER, "max-height", "Maximum height of image, in pixels.")
				.withLegacyWarning("blueprint", "bp")//
				//
				.addSlashCommand("blueprint/url", "Renders an image of the blueprint url.",
						event -> handleBlueprintSlashCommand(event))//
				.withParam(OptionType.STRING, "url", "Url containing blueprint string.")//
				.withOptionalParam(OptionType.BOOLEAN, "simple", "Set True to show just the image, no side panels.")
				.withOptionalParam(OptionType.INTEGER, "max-width", "Maximum width of image, in pixels.")
				.withOptionalParam(OptionType.INTEGER, "max-height", "Maximum height of image, in pixels.")
				//
				.addSlashCommand("blueprint/file", "Renders an image of the blueprint attachment.",
						event -> handleBlueprintSlashCommand(event))//
				.withParam(OptionType.ATTACHMENT, "file", "File containing blueprint string.")//
				.withOptionalParam(OptionType.BOOLEAN, "simple", "Set True to show just the image, no side panels.")
				.withOptionalParam(OptionType.INTEGER, "max-width", "Maximum width of image, in pixels.")
				.withOptionalParam(OptionType.INTEGER, "max-height", "Maximum height of image, in pixels.")
				//
				.addSlashCommand("json", "Provides a dump of the json data in the specified blueprint string.",
						event -> handleBlueprintJsonCommand(event))//
				.withOptionalParam(OptionType.STRING, "string", "Blueprint string.")//
				.withOptionalParam(OptionType.STRING, "url", "Url containing blueprint string.")//
				.withOptionalParam(OptionType.ATTACHMENT, "file", "File containing blueprint string.")//
				.withLegacyWarning("blueprintJSON")//
				//
				.addSlashCommand("upgrade/belts",
						"Converts all yellow belts into red belts, and all red belts into blue belts.",
						event -> handleBlueprintUpgradeBeltsCommand(event))//
				.withOptionalParam(OptionType.STRING, "string", "Blueprint string.")//
				.withOptionalParam(OptionType.STRING, "url", "Url containing blueprint string.")//
				.withOptionalParam(OptionType.ATTACHMENT, "file", "File containing blueprint string.")//
				.withLegacyWarning("blueprintUpgradeBelts")//
				//
				.addSlashCommand("items", "Prints out all of the items needed by the blueprint.",
						event -> handleBlueprintItemsCommand(event))//
				.withOptionalParam(OptionType.STRING, "string", "Blueprint string.")//
				.withOptionalParam(OptionType.STRING, "url", "Url containing blueprint string.")//
				.withOptionalParam(OptionType.ATTACHMENT, "file", "File containing blueprint string.")//
				.withLegacyWarning("blueprintItems", "bpItems")//
				//
				.addSlashCommand("raw/items", "Prints out all of the raw items needed by the blueprint.",
						event -> handleBlueprintItemsRawCommand(event))//
				.withOptionalParam(OptionType.STRING, "string", "Blueprint string.")//
				.withOptionalParam(OptionType.STRING, "url", "Url containing blueprint string.")//
				.withOptionalParam(OptionType.ATTACHMENT, "file", "File containing blueprint string.")//
				.withLegacyWarning("blueprintRawItems", "bpRawItems")//
				//
				.addSlashCommand("counts",
						"Prints out the total counts of entities, items and tiles needed by the blueprint.",
						event -> handleBlueprintTotalsCommand(event))
				.withOptionalParam(OptionType.STRING, "string", "Blueprint string.")//
				.withOptionalParam(OptionType.STRING, "url", "Url containing blueprint string.")//
				.withOptionalParam(OptionType.ATTACHMENT, "file", "File containing blueprint string.")//
				.withLegacyWarning("blueprintCounts", "bpCounts")//
				//
				//
				.addSlashCommand("book/extract",
						"Provides an collection of blueprint strings contained within the specified blueprint book.",
						event -> handleBlueprintBookExtractCommand(event))//
				.withOptionalParam(OptionType.STRING, "string", "Blueprint string.")//
				.withOptionalParam(OptionType.STRING, "url", "Url containing blueprint string.")//
				.withOptionalParam(OptionType.ATTACHMENT, "file", "File containing blueprint string.")//
				.withLegacyWarning("blueprintBookExtract")//
				//
				.addSlashCommand("book/assemble",
						"Combines all blueprints (including from other books) from multiple strings into a single book.",
						event -> handleBlueprintBookAssembleCommand(event))//
				.withOptionalParam(OptionType.STRING, "string", "Blueprint string.")//
				.withOptionalParam(OptionType.STRING, "url", "Url containing blueprint string.")//
				.withOptionalParam(OptionType.ATTACHMENT, "file", "File containing blueprint string.")//
				.withLegacyWarning("blueprintBookAssemble")//
				//
				//
				.addSlashCommand("prototype/entity", "Lua data for the specified entity prototype.",
						createPrototypeCommandHandler("entity", table.getEntities()),
						createPrototypeAutoCompleteHandler(table.getEntities()))//
				.withAutoParam(OptionType.STRING, "name", "Prototype name of the entity.")//
				.withLegacyWarning("prototypeEntity")//
				//
				.addSlashCommand("prototype/recipe", "Lua data for the specified recipe prototype.",
						createPrototypeCommandHandler("recipe", table.getRecipes()),
						createPrototypeAutoCompleteHandler(table.getRecipes()))//
				.withAutoParam(OptionType.STRING, "name", "Prototype name of the recipe.")//
				.withLegacyWarning("prototypeRecipe")//
				//
				.addSlashCommand("prototype/fluid", "Lua data for the specified fluid prototype.",
						createPrototypeCommandHandler("fluid", table.getFluids()),
						createPrototypeAutoCompleteHandler(table.getFluids()))//
				.withAutoParam(OptionType.STRING, "name", "Prototype name of the fluid.")//
				.withLegacyWarning("prototypeFluid")//
				//
				.addSlashCommand("prototype/item", "Lua data for the specified item prototype.",
						createPrototypeCommandHandler("item", table.getItems()),
						createPrototypeAutoCompleteHandler(table.getItems()))//
				.withAutoParam(OptionType.STRING, "name", "Prototype name of the item.")//
				.withLegacyWarning("prototypeItem")//
				//
				.addSlashCommand("prototype/technology", "Lua data for the specified technology prototype.",
						createPrototypeCommandHandler("technology", table.getTechnologies()),
						createPrototypeAutoCompleteHandler(table.getTechnologies()))//
				.withAutoParam(OptionType.STRING, "name", "Prototype name of the technology.")//
				.withLegacyWarning("prototypeTechnology")//
				//
				.addSlashCommand("prototype/equipment", "Lua data for the specified equipment prototype.",
						createPrototypeCommandHandler("equipment", table.getEquipments()),
						createPrototypeAutoCompleteHandler(table.getEquipments()))//
				.withAutoParam(OptionType.STRING, "name", "Prototype name of the equipment.")//
				.withLegacyWarning("prototypeEquipment")//
				//
				.addSlashCommand("prototype/tile", "Lua data for the specified tile prototype.",
						createPrototypeCommandHandler("tile", table.getTiles()),
						createPrototypeAutoCompleteHandler(table.getTiles()))//
				.withAutoParam(OptionType.STRING, "name", "Prototype name of the tile.")//
				.withLegacyWarning("prototypeTile")//
				//
				//
				.addSlashCommand("data/raw", "Lua from `data.raw` for the specified key.",
						createDataRawCommandHandler(table::getRaw))//
				.withParam(OptionType.STRING, "path", "Path to identify which key.")//
				.withLegacyWarning("dataRaw")//
				//
				//
//				.addCommand("redditCheckThings", event -> handleRedditCheckThingsCommand(event))
//				.withHelp("Troubleshooting.")//
//				.withParam(OptionType.STRING, "id", "ID to check.")//
//				.withLegacy("prototypeEntity")//
				//
				//
				//
				.withCustomSetup(builder -> {
					return builder//
							.setChunkingFilter(ChunkingFilter.NONE)//
//							.setMemberCachePolicy(MemberCachePolicy.NONE)//
//							.setDisabledIntents(GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MESSAGE_TYPING,
//									GatewayIntent.GUILD_MEMBERS)//
					;
				}).create();

		bot.startAsync().awaitRunning();

		reportingUserID = configJson.getString("reporting_user_id");
		reportingChannelID = configJson.getString("reporting_channel_id");
		hostingChannelID = configJson.getString("hosting_channel_id");

		ServiceFinder.addService(this);
		ServiceFinder.addService(WatchdogReporter.class, new WatchdogReporter() {
			@Override
			public void notifyInactive(String label) {
				TaskReporting reporting = new TaskReporting();
				reporting.addWarning(label + " has gone inactive!");
				sendReport("Watchdog", null, reporting);
			}

			@Override
			public void notifyReactive(String label) {
				TaskReporting reporting = new TaskReporting();
				reporting.addInfo(label + " is now active again!");
				sendReport("Watchdog", null, reporting);
			}
		});
	}

	public URL useDiscordForFileHosting(String fileName, byte[] fileData) throws IOException {
		TextChannel channel = bot.getJDA().getTextChannelById(hostingChannelID);
		Message message = channel.sendFile(fileData, fileName).complete();
		return new URL(message.getAttachments().get(0).getUrl());
	}

	public URL useDiscordForImageHosting(String fileName, BufferedImage image, boolean downscaleIfNeeded)
			throws IOException {
		TextChannel channel = bot.getJDA().getTextChannelById(hostingChannelID);
		Message message = channel
				.sendFile(downscaleIfNeeded ? generateDiscordFriendlyPNGImage(image) : WebUtils.getImageData(image),
						fileName)
				.complete();
		return new URL(message.getAttachments().get(0).getUrl());
	}
}
