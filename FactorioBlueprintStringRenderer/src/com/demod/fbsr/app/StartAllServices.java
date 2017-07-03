package com.demod.fbsr.app;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.json.JSONObject;

import com.demod.factorio.Config;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;

public class StartAllServices {

	private static void addServiceIfEnabled(List<Service> services, String configKey,
			Supplier<? extends Service> factory) {
		JSONObject configJson = Config.get();
		if (configJson.has(configKey) && configJson.getJSONObject(configKey).optBoolean("enabled", true)) {
			services.add(factory.get());
		}
	}

	public static void main(String[] args) {
		List<Service> services = new ArrayList<>();
		addServiceIfEnabled(services, "discord", BlueprintBotDiscordService::new);
		addServiceIfEnabled(services, "reddit", BlueprintBotRedditService::new);
		services.add(new WatchdogService());

		ServiceManager manager = new ServiceManager(services);
		manager.startAsync().awaitHealthy();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> manager.stopAsync().awaitStopped()));
	}

}
