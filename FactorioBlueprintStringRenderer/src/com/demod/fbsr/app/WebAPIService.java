package com.demod.fbsr.app;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rapidoid.http.MediaType;
import org.rapidoid.setup.App;
import org.rapidoid.setup.On;
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
import com.google.common.util.concurrent.AbstractIdleService;

import net.dv8tion.jda.api.entities.MessageEmbed.Field;

public class WebAPIService extends AbstractIdleService {

	private static final Logger LOGGER = LoggerFactory.getLogger(WebAPIService.class);

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
	protected void startUp() throws JSONException, IOException {

		ServiceFinder.findService(FactorioService.class).get().awaitRunning();

		configJson = Config.get().getJSONObject("webapi");

		String address = configJson.optString("bind", "0.0.0.0");
		int port = configJson.optInt("port", 80);

		On.address(address).port(port);

		On.post("/blueprint").serve((req, resp) -> {
			LOGGER.info("Web API POST!");
			CommandReporting reporting = new CommandReporting(
					"Web API / " + req.clientIpAddress() + " / "
							+ Optional.ofNullable(req.header("User-Agent", null)).orElse("<Unknown>"),
					null, Instant.now());
			try {
				JSONObject body = null;
				BufferedImage returnSingleImage = null;

				List<String> infos = new ArrayList<>();
				List<Entry<Optional<String>, String>> imageLinks = new ArrayList<>();

				boolean useLocalStorage = configJson.has("local-storage");

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

					List<FindBlueprintResult> blueprintStrings = BlueprintFinder.search(content);
					blueprintStrings.forEach(f -> f.failureCause.ifPresent(e -> reporting.addException(e)));
					List<BSBlueprint> blueprints = blueprintStrings.stream().filter(f -> f.blueprintString.isPresent())
							.flatMap(f -> f.blueprintString.get().findAllBlueprints().stream())
							.collect(Collectors.toList());
					List<Long> renderTimes = new ArrayList<>();

					for (BSBlueprint blueprint : blueprints) {
						try {
							RenderRequest request = new RenderRequest(blueprint, reporting);
							RenderResult result = FBSR.renderBlueprint(request);
							renderTimes.add(result.renderTime);

							if (body.optBoolean("return-single-image")) {
								returnSingleImage = result.image;
								break;
							}

							if (useLocalStorage) {
								File localStorageFolder = new File(configJson.getString("local-storage"));
								String imageLink = saveToLocalStorage(localStorageFolder, result.image);
								imageLinks.add(new SimpleEntry<>(blueprint.label, imageLink));
							} else {
								// TODO links expire, need a new approach
								Optional<BlueprintBotDiscordService> discordService = ServiceFinder
										.findService(BlueprintBotDiscordService.class);
								if (discordService.isPresent()) {
									imageLinks
											.add(new SimpleEntry<>(blueprint.label,
													discordService.get().useDiscordForFileHosting(
															WebUtils.formatBlueprintFilename(blueprint.label, "png"),
															result.image).toString()));
								}
							}
						} catch (Exception e) {
							reporting.addException(e);
						}
					}

					if (!renderTimes.isEmpty()) {
						reporting.addField(new Field("Render Time",
								renderTimes.stream().mapToLong(l -> l).sum() + " ms"
										+ (renderTimes.size() > 1
												? (" [" + renderTimes.stream().map(Object::toString)
														.collect(Collectors.joining(", ")) + "]")
												: ""),
								true));
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

					if (!reporting.getExceptionsWithBlame().isEmpty()) {
						resp.code(400);
						infos.add("There was a problem completing your request.");
						reporting.getExceptionsWithBlame().forEach(e -> e.getException().printStackTrace());
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

		LOGGER.info("Web API Initialized at {}:{}", address, port);
	}

}
