package com.demod.fbsr.app;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.json.JSONArray;
import org.json.JSONObject;
import org.rapidoid.http.MediaType;
import org.rapidoid.setup.App;
import org.rapidoid.setup.On;

import com.demod.dcba.CommandReporting;
import com.demod.factorio.Config;
import com.demod.factorio.Utils;
import com.demod.fbsr.Blueprint;
import com.demod.fbsr.BlueprintFinder;
import com.demod.fbsr.BlueprintStringData;
import com.demod.fbsr.FBSR;
import com.demod.fbsr.WebUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AbstractIdleService;

import net.dv8tion.jda.api.entities.MessageEmbed.Field;

public class WebAPIService extends AbstractIdleService {

	private JSONObject configJson;

	private String saveToLocalStorage(File folder, BufferedImage image) throws IOException {
		if (!folder.exists()) {
			folder.mkdirs();
		}

		File imageFile;
		long id = System.currentTimeMillis();
		String fileName;
		while ((imageFile = new File(folder, fileName = "Blueprint" + id + ".png")).exists()) {
			id++;
		}

		ImageIO.write(image, "PNG", imageFile);

		return fileName;
	}

	@Override
	protected void shutDown() {
		ServiceFinder.removeService(this);

		App.shutdown();
	}

	@Override
	protected void startUp() {
		ServiceFinder.addService(this);

		configJson = Config.get().getJSONObject("webapi");

		String address = configJson.optString("bind", "0.0.0.0");
		int port = configJson.optInt("port", 80);

		On.address(address).port(port);

		On.post("/blueprint").serve((req, resp) -> {
			System.out.println("Web API POST!");
			CommandReporting reporting = new CommandReporting("Web API / " + req.clientIpAddress() + " / "
					+ Optional.ofNullable(req.header("User-Agent", null)).orElse("<Unknown>"), null);
			try {
				JSONObject body = null;
				BufferedImage returnSingleImage = null;

				List<String> infos = new ArrayList<>();
				List<Entry<Optional<String>, String>> imageLinks = new ArrayList<>();

				boolean useLocalStorage = configJson.optBoolean("use-local-storage", false);

				try {
					if (req.body() == null) {
						resp.code(400);
						resp.plain("Body is empty!");
						reporting.addException(new IllegalArgumentException("Body is empty!"));
						return resp;
					}

					try {
						body = new JSONObject(new String(req.body()));
					} catch (Exception e) {
						reporting.addException(e);
						resp.code(400);
						resp.plain("Malformed JSON: " + e.getMessage());
						return resp;
					}
					reporting.setCommand(body.toString(2));

					/*
					 * { "blueprint": "0e...", (required) "max-width": 1234, "max-height": 1234,
					 * "show-info-panels": false } | v { "info": [ "message 1!", "message 2!", ...
					 * ], "images": [ { "label": "Blueprint Label", "link":
					 * "https://cdn.discordapp.com/..." (or) "1563569893008.png" } ] }
					 */

					String content = body.getString("blueprint");

					List<BlueprintStringData> blueprintStrings = BlueprintFinder.search(content, reporting);
					List<Blueprint> blueprints = blueprintStrings.stream().flatMap(s -> s.getBlueprints().stream())
							.collect(Collectors.toList());

					for (Blueprint blueprint : blueprints) {
						try {
							BufferedImage image = FBSR.renderBlueprint(blueprint, reporting, body);

							if (body.optBoolean("return-single-image")) {
								returnSingleImage = image;
								break;
							}

							if (useLocalStorage) {
								File localStorageFolder = new File(configJson.getString("local-storage"));
								String imageLink = saveToLocalStorage(localStorageFolder, image);
								imageLinks.add(new SimpleEntry<>(blueprint.getLabel(), imageLink));
							} else {
								imageLinks.add(new SimpleEntry<>(blueprint.getLabel(),
										WebUtils.uploadToHostingService("blueprint.png", image).toString()));
							}
						} catch (Exception e) {
							reporting.addException(e);
						}
					}

					List<Long> renderTimes = blueprintStrings.stream().flatMap(d -> d.getBlueprints().stream())
							.flatMap(b -> (b.getRenderTime().isPresent() ? Arrays.asList(b.getRenderTime().getAsLong())
									: ImmutableList.<Long>of()).stream())
							.collect(Collectors.toList());
					if (!renderTimes.isEmpty()) {
						reporting.addField(new Field("Render Time",
								renderTimes.stream().mapToLong(l -> l).sum() + " ms"
										+ (renderTimes.size() > 1
												? (" [" + renderTimes.stream().map(Object::toString)
														.collect(Collectors.joining(", ")) + "]")
												: ""),
								true));
					}

					if (blueprintStrings.stream()
							.anyMatch(d -> d.getBlueprints().stream().anyMatch(b -> b.isModsDetected()))) {
						infos.add("(Modded features are shown as question marks)");
					}
				} catch (Exception e) {
					reporting.addException(e);
				}

				if (returnSingleImage != null) {
					resp.contentType(MediaType.IMAGE_PNG);
					try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
						ImageIO.write(returnSingleImage, "PNG", baos);
						baos.flush();
						resp.body(baos.toByteArray());
					}
					return resp;

				} else {

					JSONObject result = new JSONObject();
					Utils.terribleHackToHaveOrderedJSONObject(result);

					if (!reporting.getExceptions().isEmpty()) {
						resp.code(400);
						infos.add("There was a problem completing your request.");
						reporting.getExceptions().forEach(Exception::printStackTrace);
					}

					if (!infos.isEmpty()) {
						result.put("info", new JSONArray(infos));
					}

					if (imageLinks.size() == 1 && !useLocalStorage) {
						reporting.setImageURL(imageLinks.get(0).getValue());
					}

					if (!imageLinks.isEmpty()) {
						JSONArray images = new JSONArray();
						for (Entry<Optional<String>, String> pair : imageLinks) {
							JSONObject image = new JSONObject();
							Utils.terribleHackToHaveOrderedJSONObject(image);
							pair.getKey().ifPresent(l -> image.put("label", l));
							image.put("link", pair.getValue());
							images.put(image);
						}
						result.put("images", images);
					}

					resp.contentType(MediaType.JSON);
					String responseBody = result.toString(2);
					resp.body(responseBody.getBytes());

					reporting.addField(new Field("Response", responseBody, false));

					return resp;
				}

			} finally {
				ServiceFinder.findService(BlueprintBotDiscordService.class)
						.ifPresent(s -> s.getBot().submitReport(reporting));
			}

		});

		System.out.println("Web API Initialized at " + address + ":" + port);
	}

}
