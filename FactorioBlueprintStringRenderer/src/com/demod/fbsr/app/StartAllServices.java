package com.demod.fbsr.app;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.json.JSONObject;

import com.demod.factorio.Config;
import com.demod.fbsr.app.PluginFinder.Plugin;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.common.util.concurrent.ServiceManager.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StartAllServices {

	private static final Logger LOGGER = LoggerFactory.getLogger(StartAllServices.class);

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
		addServiceIfEnabled(services, "webapi", WebAPIService::new);
		addServiceIfEnabled(services, "watchdog", WatchdogService::new);
		addServiceIfEnabled(services, "logging", LoggingService::new);

		ServiceManager manager = new ServiceManager(services);
		manager.addListener(new Listener() {
			@Override
			public void failure(Service service) {
				LOGGER.info("SERVICE FAILURE: {}", service.getClass().getSimpleName());
				service.failureCause().printStackTrace();
			}

			@Override
			public void healthy() {
				LOGGER.info("ALL SERVICES ARE HEALTHY!");
			}

			@Override
			public void stopped() {
				LOGGER.info("ALL SERVICES HAVE STOPPED!");
			}
		});

		manager.startAsync().awaitHealthy();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> manager.stopAsync().awaitStopped()));

		PluginFinder.loadPlugins().forEach(Plugin::run);
	}

}
