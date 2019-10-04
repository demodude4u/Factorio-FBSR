package com.demod.fbsr.app;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
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

import com.demod.factorio.Config;
import com.demod.factorio.Utils;
import com.demod.fbsr.Blueprint;
import com.demod.fbsr.BlueprintFinder;
import com.demod.fbsr.BlueprintStringData;
import com.demod.fbsr.FBSR;
import com.demod.fbsr.TaskReporting;
import com.demod.fbsr.WebUtils;
import com.google.common.util.concurrent.AbstractIdleService;

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
			TaskReporting reporting = new TaskReporting();
			try {
				try {
					if (req.body() == null) {
						resp.code(400);
						resp.plain("Body is empty!");
						reporting.addException(new IllegalArgumentException("Body is empty!"));
						return resp;
					}

					JSONObject body;
					try {
						body = new JSONObject(new String(req.body()));
					} catch (Exception e) {
						reporting.addException(e);
						resp.code(400);
						resp.plain("Malformed JSON: " + e.getMessage());
						return resp;
					}
					reporting.setContext(body.toString(2));

					/*
					 * 	{
					 * 		"blueprint": "0e...", 		(required)
					 * 		"max-width": 1234,
					 * 		"max-height": 1234,
					 * 		"show-info-panels": false
					 * 	}
					 * 				|
					 * 				v
					 * {
					 * 		"info": [
					 * 			"message 1!", "message 2!", ...
					 * 		],
					 * 		"images": [
					 * 			{
					 * 				"label": "Blueprint Label",
					 * 				"link": "https://cdn.discordapp.com/..." (or) "1563569893008.png"
					 * 			}
					 * 		]
					 * }
					 */

					String content = body.getString("blueprint");

					List<BlueprintStringData> blueprintStrings = BlueprintFinder.search(content, reporting);
					List<Blueprint> blueprints = blueprintStrings.stream().flatMap(s -> s.getBlueprints().stream())
							.collect(Collectors.toList());

					for (Blueprint blueprint : blueprints) {
						try {
							BufferedImage image = FBSR.renderBlueprint(blueprint, reporting, body);
							if (configJson.optBoolean("use-local-storage", false)) {
								File localStorageFolder = new File(configJson.getString("local-storage"));
								String imageLink = saveToLocalStorage(localStorageFolder, image);
								reporting.addImage(blueprint.getLabel(), imageLink);
								reporting.addLink(imageLink);
							} else {
								reporting.addImage(blueprint.getLabel(),
										WebUtils.uploadToHostingService("blueprint.png", image).toString());
							}
						} catch (Exception e) {
							reporting.addException(e);
						}
					}
				} catch (Exception e) {
					reporting.addException(e);
				}

				JSONObject result = new JSONObject();
				Utils.terribleHackToHaveOrderedJSONObject(result);

				if (!reporting.getExceptions().isEmpty()) {
					reporting.addInfo(
							"There was a problem completing your request. I have contacted my programmer to fix it for you!");
				}

				if (!reporting.getInfo().isEmpty()) {
					result.put("info", new JSONArray(reporting.getInfo()));
				}

				if (!reporting.getImages().isEmpty()) {
					JSONArray images = new JSONArray();
					for (Entry<Optional<String>, String> pair : reporting.getImages()) {
						JSONObject image = new JSONObject();
						Utils.terribleHackToHaveOrderedJSONObject(image);
						pair.getKey().ifPresent(l -> image.put("label", l));
						image.put("link", pair.getValue());
						images.put(image);
					}
					result.put("images", images);
				}

				resp.contentType(MediaType.JSON);
				resp.body(result.toString(2).getBytes());

				return resp;

			} finally {
				ServiceFinder.findService(BlueprintBotDiscordService.class)
						.ifPresent(s -> s.sendReport(
								"Web API / " + req.clientIpAddress() + " / "
										+ Optional.ofNullable(req.header("User-Agent", null)).orElse("<Unknown>"),
								null, reporting));
			}

		});

		System.out.println("Web API Initialized at " + address + ":" + port);
	}

}
