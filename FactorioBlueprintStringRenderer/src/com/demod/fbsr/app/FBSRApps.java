package com.demod.fbsr.app;

import java.io.IOException;
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

public class FBSRApps {
	public static volatile String status = "idle";

	private static final Logger LOGGER = LoggerFactory.getLogger(FBSRApps.class);

	private static volatile ServiceManager manager = null;
	private static volatile boolean started = false;

	private static void addServiceIfEnabled(List<Service> services, String configKey,
			Supplier<? extends Service> factory) {
		JSONObject configJson = Config.get();
		if (configJson.has(configKey) && configJson.getJSONObject(configKey).optBoolean("enabled", true)) {
			services.add(factory.get());
		}
	}

	public static synchronized boolean start() {
		if (started) {
			LOGGER.info("Services are already started.");
			return true;
		}

		List<Service> services = new ArrayList<>();
		addServiceIfEnabled(services, "discord", BlueprintBotDiscordService::new);
		addServiceIfEnabled(services, "webapi", WebAPIService::new);
		addServiceIfEnabled(services, "logging", LoggingService::new);
		services.add(new RPCService());
		services.add(new FactorioService());
		
		for (Service service : services) {
			ServiceFinder.addService(service);
		}

		status = "starting";
		manager = new ServiceManager(services);
		manager.addListener(new Listener() {
			@Override
			public void failure(Service service) {
				status = "failed";
				LOGGER.info("SERVICE FAILURE: {}", service.getClass().getSimpleName());
				// service.failureCause().printStackTrace();
			}

			@Override
			public void healthy() {
				status = "healthy";
				LOGGER.info("ALL SERVICES ARE HEALTHY!");
			}

			@Override
			public void stopped() {
				status = "stopped";
				LOGGER.info("ALL SERVICES HAVE STOPPED!");
			}
		});

		try {
			manager.startAsync().awaitHealthy();
		} catch (IllegalStateException e) {
			System.out.println(e.getMessage());
			return false;
		}
		Runtime.getRuntime().addShutdownHook(new Thread(() -> manager.stopAsync().awaitStopped()));

		PluginFinder.loadPlugins().forEach(Plugin::run);

		started = true;
		return true;
	}

	public static synchronized boolean isStarted() {
		return started;
	}

	public static synchronized boolean stopAsync() {
		if (!started) {
			LOGGER.info("Services are not started, nothing to stop.");
			return true;
		}
		
		manager.stopAsync();
		started = false;
		return true;
	}

	public static synchronized boolean stop() {
		if (!started) {
			LOGGER.info("Services are not started, nothing to stop.");
			return true;
		}

		manager.stopAsync().awaitStopped();
		started = false;
		return true;
	}

	public static synchronized boolean waitForStopped(boolean forceStopOnKeyPress) {
		if (!started) {
			return true;
		}

		if (forceStopOnKeyPress) {
			System.out.println("Press Any Key to stop...");
			try {
				System.in.read();
				System.out.println("Stopping services...");
				manager.stopAsync();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		manager.awaitStopped();
		started = false;
		return true;
	}

}
