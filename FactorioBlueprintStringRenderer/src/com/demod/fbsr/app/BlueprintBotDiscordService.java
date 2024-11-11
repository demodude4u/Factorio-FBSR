package com.demod.fbsr.app;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
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
import com.demod.dcba.CommandReporting;
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
import com.demod.fbsr.WebUtils;
import com.demod.fbsr.app.WatchdogService.WatchdogReporter;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AbstractIdleService;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.FileUpload;

public class BlueprintBotDiscordService extends AbstractIdleService {

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

	private String hostingChannelID;

	private SlashCommandHandler createDataRawCommandHandler(Function<String[], Optional<LuaValue>> query) {
		return (event) -> {
			String key = event.getParamString("path");
			String[] path = key.split("\\.");
			Optional<LuaValue> lua = query.apply(path);
			if (!lua.isPresent()) {
				event.reply("I could not find a lua table for the path [`"
						+ Arrays.asList(path).stream().collect(Collectors.joining(", ")) + "`] :frowning:");
				return;
			}
			sendLuaDumpFile(event, "raw", key, lua.get());
		};
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
			String search = event.getParamString("name");
			Optional<? extends DataPrototype> prototype = Optional.ofNullable(map.get(search));
			if (!prototype.isPresent()) {
				LevenshteinDistance levenshteinDistance = LevenshteinDistance.getDefaultInstance();
				List<String> suggestions = map.keySet().stream()
						.map(k -> new SimpleEntry<>(k, levenshteinDistance.apply(search, k)))
						.sorted((p1, p2) -> Integer.compare(p1.getValue(), p2.getValue())).limit(5).map(p -> p.getKey())
						.collect(Collectors.toList());
				event.reply("I could not find the " + category + " prototype for `" + search
						+ "`. :frowning:\nDid you mean:\n"
						+ suggestions.stream().map(s -> "\t - " + s).collect(Collectors.joining("\n")));
				return;
			}

			sendLuaDumpFile(event, category, prototype.get().getName(), prototype.get().lua());
		};
	}

	private void findDebugOptions(CommandReporting reporting, String content, JSONObject options) {
		Matcher matcher = debugPattern.matcher(content);
		while (matcher.find()) {
			String fieldName = matcher.group(1);
			options.put("debug-" + fieldName, true);
		}
	}

	public DiscordBot getBot() {
		return bot;
	}

	private void handleBlueprintBookAssembleCommand(SlashCommandEvent event) {
		String content = event.getCommandString();

		Optional<Attachment> attachment = event.optParamAttachment("file");
		if (attachment.isPresent()) {
			content += " " + attachment.get().getUrl();
		}

		List<BlueprintStringData> blueprintStrings = BlueprintFinder.search(content, event.getReporting());

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
				event.replyFile(BlueprintStringData.encode(json).getBytes(), "blueprintBook.txt");
			} catch (Exception e) {
				event.getReporting().addException(e);
			}

		} else {
			event.replyIfNoException("No blueprint found!");
		}
	}

	private void handleBlueprintBookExtractCommand(SlashCommandEvent event) {
		String content = event.getCommandString();

		Optional<Attachment> attachment = event.optParamAttachment("file");
		if (attachment.isPresent()) {
			content += " " + attachment.get().getUrl();
		}

		List<BlueprintStringData> blueprintStringDatas = BlueprintFinder.search(content, event.getReporting());

		List<Blueprint> blueprints = blueprintStringDatas.stream().flatMap(bs -> bs.getBlueprints().stream())
				.collect(Collectors.toList());
		List<Entry<String, String>> links = new ArrayList<>();
		for (Blueprint blueprint : blueprints) {
			try {
				blueprint.json().remove("index");

				String url = WebUtils.uploadToHostingService("blueprint.txt",
						BlueprintStringData.encode(blueprint.json()).getBytes());
				links.add(new SimpleEntry<>(url, blueprint.getLabel().orElse(null)));
			} catch (Exception e) {
				event.getReporting().addException(e);
			}
		}

		List<EmbedBuilder> embedBuilders = new ArrayList<>();

		if (!links.isEmpty()) {
			ArrayDeque<String> lines = links.stream()
					.map(p -> (p.getValue() != null && !p.getValue().isEmpty())
							? ("[" + p.getValue() + "](" + p.getKey() + ")")
							: p.getKey())
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

		List<MessageEmbed> embeds = embedBuilders.stream().map(EmbedBuilder::build).collect(Collectors.toList());
		for (MessageEmbed embed : embeds) {
			event.replyEmbed(embed);
		}
	}

	private void handleBlueprintItemsCommand(SlashCommandEvent event) throws IOException {
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

		List<BlueprintStringData> blueprintStringDatas = BlueprintFinder.search(content, event.getReporting());

		Map<String, Double> totalItems = new LinkedHashMap<>();
		for (BlueprintStringData blueprintStringData : blueprintStringDatas) {
			for (Blueprint blueprint : blueprintStringData.getBlueprints()) {
				Map<String, Double> items = FBSR.generateTotalItems(table, blueprint);
				items.forEach((k, v) -> {
					totalItems.compute(k, ($, old) -> old == null ? v : old + v);
				});
			}
		}

		if (!totalItems.isEmpty()) {
			String responseContent = totalItems.entrySet().stream()
					.sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
					.map(e -> table.getWikiItemName(e.getKey()) + ": " + RenderUtils.fmtDouble2(e.getValue()))
					.collect(Collectors.joining("\n"));

			String response = "```ldif\n" + responseContent + "```";
			if (response.length() < 2000) {
				event.reply(response);
			} else {
				event.replyFile(responseContent.getBytes(), "items.txt");
			}
		} else if (blueprintStringDatas.stream().anyMatch(d -> !d.getBlueprints().isEmpty())) {
			event.replyIfNoException("I couldn't find any items!");
		} else {
			event.replyIfNoException("No blueprint found!");
		}
	}

	private void handleBlueprintItemsRawCommand(SlashCommandEvent event) throws IOException {
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

		List<BlueprintStringData> blueprintStringDatas = BlueprintFinder.search(content, event.getReporting());

		Map<String, Double> totalItems = new LinkedHashMap<>();
		for (BlueprintStringData blueprintStringData : blueprintStringDatas) {
			for (Blueprint blueprint : blueprintStringData.getBlueprints()) {
				Map<String, Double> items = FBSR.generateTotalItems(table, blueprint);
				items.forEach((k, v) -> {
					totalItems.compute(k, ($, old) -> old == null ? v : old + v);
				});
			}
		}

		Map<String, Double> rawItems = FBSR.generateTotalRawItems(table, table.getRecipes(), totalItems);

		if (!rawItems.isEmpty()) {
			String responseContent = rawItems.entrySet().stream().sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
					.map(e -> table.getWikiItemName(e.getKey()) + ": " + RenderUtils.fmtDouble2(e.getValue()))
					.collect(Collectors.joining("\n"));

			String response = "```ldif\n" + responseContent + "```";
			if (response.length() < 2000) {
				event.reply(response);
			} else {
				event.replyFile(responseContent.getBytes(), "raw-items.txt");
			}
		} else if (blueprintStringDatas.stream().anyMatch(d -> !d.getBlueprints().isEmpty())) {
			event.replyIfNoException("I couldn't find any items!");
		} else {
			event.replyIfNoException("No blueprint found!");
		}
	}

	private void handleBlueprintJsonCommand(SlashCommandEvent event) throws JSONException, IOException {
		String content = event.getCommandString();

		Optional<Attachment> attachment = event.optParamAttachment("file");
		if (attachment.isPresent()) {
			content += " " + attachment.get().getUrl();
		}

		List<String> results = BlueprintFinder.searchRaw(content, event.getReporting());
		if (!results.isEmpty()) {
			byte[] bytes = BlueprintStringData.decode(results.get(0)).toString(2).getBytes();
			if (results.size() == 1) {
				event.replyFile(bytes, "blueprint.json");
			} else {
				try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
						ZipOutputStream zos = new ZipOutputStream(baos)) {
					for (int i = 0; i < results.size(); i++) {
						try {
							String blueprintString = results.get(i);
							zos.putNextEntry(new ZipEntry("blueprint " + (i + 1) + ".json"));
							zos.write(BlueprintStringData.decode(blueprintString).toString(2).getBytes());
						} catch (Exception e) {
							event.getReporting().addException(e);
						}
					}
					zos.close();
					byte[] zipData = baos.toByteArray();
					event.replyFile(zipData, "blueprint JSON files.zip");
				}
			}
		} else {
			event.replyIfNoException("No blueprint found!");
		}
	}

	private void handleBlueprintMessageCommand(MessageCommandEvent event) throws IOException {
		String content = event.getMessage().getContentDisplay();

		for (Attachment attachment : event.getMessage().getAttachments()) {
			content += " " + attachment.getUrl();
		}

		JSONObject options = new JSONObject();
		findDebugOptions(event.getReporting(), content, options);

		List<BlueprintStringData> blueprintStringDatas = BlueprintFinder.search(content, event.getReporting());
		List<EmbedBuilder> embedBuilders = processBlueprints(blueprintStringDatas, event.getReporting(), options);

		embedBuilders.get(0).setAuthor(event.getMessage().getAuthor().getName(), event.getMessage().getJumpUrl(),
				event.getMessage().getAuthor().getEffectiveAvatarUrl());

		if (blueprintStringDatas.stream().anyMatch(d -> d.getBlueprints().stream().anyMatch(b -> b.isModsDetected()))) {
			embedBuilders.get(embedBuilders.size() - 1).setFooter("(Modded features are shown as question marks)");
		}

		if (event.getReporting().isNewFormatDetected()) {
			embedBuilders.get(embedBuilders.size() - 1).setFooter("(Space Age is not supported yet, sorry!)");
		}

		List<MessageEmbed> embeds = embedBuilders.stream().map(EmbedBuilder::build).collect(Collectors.toList());
		for (MessageEmbed embed : embeds) {
			event.replyEmbed(embed);
		}
	}

	private void handleBlueprintSlashCommand(SlashCommandEvent event) throws IOException {
		String content = event.getCommandString();

		Optional<Attachment> attachment = event.optParamAttachment("file");
		if (attachment.isPresent()) {
			content += " " + attachment.get().getUrl();
		}

		JSONObject options = new JSONObject();
		findDebugOptions(event.getReporting(), content, options);
		event.optParamBoolean("simple").ifPresent(b -> options.put("show-info-panels", !b));
		event.optParamLong("max-width").ifPresent(l -> options.put("max-width", l.intValue()));
		event.optParamLong("max-height").ifPresent(l -> options.put("max-height", l.intValue()));

		List<BlueprintStringData> blueprintStringDatas = BlueprintFinder.search(content, event.getReporting());
		List<EmbedBuilder> embedBuilders = processBlueprints(blueprintStringDatas, event.getReporting(), options);

		if (blueprintStringDatas.stream().anyMatch(d -> d.getBlueprints().stream().anyMatch(b -> b.isModsDetected()))) {
			embedBuilders.get(embedBuilders.size() - 1).setFooter("(Modded features are shown as question marks)");
		}

		if (event.getReporting().isNewFormatDetected()) {
			embedBuilders.get(embedBuilders.size() - 1).setFooter("(Space Age is not supported yet, sorry!)");
		}

		List<MessageEmbed> embeds = embedBuilders.stream().map(EmbedBuilder::build).collect(Collectors.toList());
		for (MessageEmbed embed : embeds) {
			event.replyEmbed(embed);
		}
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

		List<BlueprintStringData> blueprintStringDatas = BlueprintFinder.search(content, event.getReporting());

		Map<String, Double> totalItems = new LinkedHashMap<>();
		for (BlueprintStringData blueprintStringData : blueprintStringDatas) {
			for (Blueprint blueprint : blueprintStringData.getBlueprints()) {
				Map<String, Double> items = FBSR.generateSummedTotalItems(table, blueprint);
				items.forEach((k, v) -> {
					totalItems.compute(k, ($, old) -> old == null ? v : old + v);
				});
			}
		}

		if (!totalItems.isEmpty()) {
			String responseContent = totalItems.entrySet().stream()
					.sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
					.map(e -> e.getKey() + ": " + RenderUtils.fmtDouble2(e.getValue()))
					.collect(Collectors.joining("\n"));

			String response = "```ldif\n" + responseContent + "```";
			if (response.length() < 2000) {
				event.reply(response);
			} else {
				event.replyFile(responseContent.getBytes(), "totals.txt");
			}
		} else if (blueprintStringDatas.stream().anyMatch(d -> !d.getBlueprints().isEmpty())) {
			event.replyIfNoException("I couldn't find any items!");
		} else {
			event.replyIfNoException("No blueprint found!");
		}
	}

	private void handleBlueprintUpgradeBeltsCommand(SlashCommandEvent event) {
		String content = event.getCommandString();

		Optional<Attachment> attachment = event.optParamAttachment("file");
		if (attachment.isPresent()) {
			content += " " + attachment.get().getUrl();
		}

		List<BlueprintStringData> blueprintStringDatas = BlueprintFinder.search(content, event.getReporting());

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
			for (BlueprintStringData blueprintStringData : blueprintStringDatas) {
				try {
					event.replyFile(BlueprintStringData.encode(blueprintStringData.json()).getBytes(), "blueprint.txt");
				} catch (IOException e) {
					event.getReporting().addException(e);
				}
			}
			event.reply("Upgraded " + upgradedCount + " entities.");
		} else if (blueprintStringDatas.stream().anyMatch(d -> !d.getBlueprints().isEmpty())) {
			event.replyIfNoException("I couldn't find anything to upgrade!");
		} else {
			event.replyIfNoException("No blueprint found!");
		}
	}

	private List<EmbedBuilder> processBlueprints(List<BlueprintStringData> blueprintStrings, CommandReporting reporting,
			JSONObject options) throws IOException {

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

		List<Long> renderTimes = blueprintStrings.stream().flatMap(d -> d.getBlueprints().stream())
				.flatMap(b -> (b.getRenderTime().isPresent() ? Arrays.asList(b.getRenderTime().getAsLong())
						: ImmutableList.<Long>of()).stream())
				.collect(Collectors.toList());
		if (!renderTimes.isEmpty()) {
			reporting.addField(new Field("Render Time", renderTimes.stream().mapToLong(l -> l).sum() + " ms"
					+ (renderTimes.size() > 1
							? (" [" + renderTimes.stream().map(Object::toString).collect(Collectors.joining(", "))
									+ "]")
							: ""),
					true));
		}

		if (images.size() == 0) {
			embedBuilders.add(new EmbedBuilder().setDescription("Blueprint not found!"));

		} else if (images.size() == 1) {
			Entry<Optional<String>, BufferedImage> entry = images.get(0);
			BufferedImage image = entry.getValue();
			String url = WebUtils.uploadToHostingService("blueprint.png", image);
			EmbedBuilder builder = new EmbedBuilder();
			builder.setImage(url);
			embedBuilders.add(builder);

		} else {
			List<Entry<String, String>> links = new ArrayList<>();
			for (Entry<Optional<String>, BufferedImage> entry : images) {
				BufferedImage image = entry.getValue();
				links.add(new SimpleEntry<>(WebUtils.uploadToHostingService("blueprint.png", image),
						entry.getKey().orElse("")));
			}

			ArrayDeque<String> lines = new ArrayDeque<>();
			for (int i = 0; i < links.size(); i++) {
				Entry<String, String> entry = links.get(i);
				if (entry.getValue() != null && !entry.getValue().trim().isEmpty()) {
					lines.add("[" + entry.getValue().trim() + "](" + entry.getKey() + ")");
				} else {
					lines.add("[Blueprint Image " + (i + 1) + "](" + entry.getKey() + ")");
				}
			}

			while (!lines.isEmpty()) {
				EmbedBuilder builder = new EmbedBuilder();
				for (int i = 0; i < 3 && !lines.isEmpty(); i++) {
					StringBuilder description = new StringBuilder();
					while (!lines.isEmpty()) {
						if (description.length() + lines.peek().length() + 1 < MessageEmbed.VALUE_MAX_LENGTH) {
							description.append(lines.pop()).append('\n');
						} else {
							break;
						}
					}
					builder.addField("", description.toString(), true);
				}
				embedBuilders.add(builder);
			}
		}

		if (!blueprintStrings.isEmpty() && !embedBuilders.isEmpty()) {
			List<String> links = new ArrayList<>();
			for (int i = 0; i < blueprintStrings.size(); i++) {
				BlueprintStringData blueprintString = blueprintStrings.get(i);
				String label = blueprintString.getLabel().orElse(
						(blueprintString.getBlueprints().size() > 1 ? "Blueprint Book " : "Blueprint ") + (i + 1));
				String link = WebUtils.uploadToHostingService("blueprint.txt", blueprintString.toString().getBytes());
				links.add("[" + label + "](" + link + ")");
			}

			embedBuilders.get(0).getFields().add(0,
					new Field("Blueprint String" + (blueprintStrings.size() > 1 ? "s" : ""),
							links.stream().collect(Collectors.joining("\n")), false));
		}

		return embedBuilders;
	}

	private void sendLuaDumpFile(EventReply event, String category, String name, LuaValue lua) throws IOException {
		JSONObject json = new JSONObject();
		Utils.terribleHackToHaveOrderedJSONObject(json);
		json.put("name", name);
		json.put("category", category);
		json.put("version", FBSR.getVersion());
		json.put("data", Utils.<JSONObject>convertLuaToJson(lua));
		String fileName = category + "_" + name + "_dump_" + FBSR.getVersion() + ".json";
		String url = WebUtils.uploadToHostingService(fileName, json.toString(2).getBytes());
		event.reply(category + " " + name + " lua dump: [" + fileName + "](" + url + ")");
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
		FBSR.initialize();
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
				.addSlashCommand("bp/string", "Renders an image of the blueprint string.",
						event -> handleBlueprintSlashCommand(event))//
				.withParam(OptionType.STRING, "string", "Blueprint string.")//
				.withOptionalParam(OptionType.BOOLEAN, "simple", "Set True to show just the image, no side panels.")
				.withOptionalParam(OptionType.INTEGER, "max-width", "Maximum width of image, in pixels.")
				.withOptionalParam(OptionType.INTEGER, "max-height", "Maximum height of image, in pixels.")
				.withLegacyWarning("blueprint", "bp")//
				//
				.addSlashCommand("bp/url", "Renders an image of the blueprint url.",
						event -> handleBlueprintSlashCommand(event))//
				.withParam(OptionType.STRING, "url", "Url containing blueprint string.")//
				.withOptionalParam(OptionType.BOOLEAN, "simple", "Set True to show just the image, no side panels.")
				.withOptionalParam(OptionType.INTEGER, "max-width", "Maximum width of image, in pixels.")
				.withOptionalParam(OptionType.INTEGER, "max-height", "Maximum height of image, in pixels.")
				//
				.addSlashCommand("bp/file", "Renders an image of the blueprint attachment.",
						event -> handleBlueprintSlashCommand(event))//
				.withParam(OptionType.ATTACHMENT, "file", "File containing blueprint string.")//
				.withOptionalParam(OptionType.BOOLEAN, "simple", "Set True to show just the image, no side panels.")
				.withOptionalParam(OptionType.INTEGER, "max-width", "Maximum width of image, in pixels.")
				.withOptionalParam(OptionType.INTEGER, "max-height", "Maximum height of image, in pixels.")
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

		hostingChannelID = configJson.getString("hosting_channel_id");

		ServiceFinder.addService(this);
		ServiceFinder.addService(WatchdogReporter.class, new WatchdogReporter() {
			@Override
			public void notifyInactive(String label) {
				CommandReporting reporting = new CommandReporting("Watchdog Reporter", null, null);
				reporting.addWarning(label + " has gone inactive!");
				bot.submitReport(reporting);
			}

			@Override
			public void notifyReactive(String label) {
				CommandReporting reporting = new CommandReporting("Watchdog Reporter", null, null);
				reporting.addWarning(label + " is now active again!");
				bot.submitReport(reporting);
			}
		});
	}

	public URL useDiscordForFileHosting(String fileName, byte[] fileData) throws IOException {
		TextChannel channel = bot.getJDA().getTextChannelById(hostingChannelID);
		Message message = channel.sendFiles(FileUpload.fromData(fileData, fileName)).complete();
		return new URL(message.getAttachments().get(0).getUrl());
	}
}
