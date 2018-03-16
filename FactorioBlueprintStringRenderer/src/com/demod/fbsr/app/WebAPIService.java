package com.demod.fbsr.app;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

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

	@Override
	protected void shutDown() throws Exception {
		ServiceFinder.removeService(this);

		App.shutdown();
	}

	@Override
	protected void startUp() throws Exception {
		ServiceFinder.addService(this);

		configJson = Config.get().getJSONObject("webapi");

		int port = configJson.optInt("port", 80);

		On.address(configJson.optString("bind", "0.0.0.0")).port(port);

		On.post("/blueprint").serve((req, resp) -> {
			TaskReporting reporting = new TaskReporting();
			try {
				String content = Optional.ofNullable(req.body()).map(String::new).orElse("");
				reporting.setContext(content);

				List<BlueprintStringData> blueprintStrings = BlueprintFinder.search(content, reporting);
				List<Blueprint> blueprints = blueprintStrings.stream().flatMap(s -> s.getBlueprints().stream())
						.collect(Collectors.toList());

				for (Blueprint blueprint : blueprints) {
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
					image.put("url", pair.getValue());
					images.put(image);
				}
				result.put("images", images);
			}

			ServiceFinder.findService(BlueprintBotDiscordService.class)
					.ifPresent(s -> s.sendReport(
							"Web API / " + req.clientIpAddress() + " / "
									+ Optional.ofNullable(req.header("User-Agent", null)).orElse("<Unknown>"),
							null, reporting));

			resp.contentType(MediaType.JSON);
			resp.body(result.toString(2).getBytes());

			return resp;
		});

		System.out.println("Web API Initialized!");
	}

}
