package com.demod.fbsr.app;

import java.util.Arrays;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;

public class StartAllServices {

	public static void main(String[] args) {
		ServiceManager manager = new ServiceManager(Arrays.asList(new Service[] { //
				new BlueprintBotDiscordService(), //
				new BlueprintBotRedditService(), //
				new WatchdogService(),//
		}));

		manager.startAsync().awaitHealthy();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> manager.stopAsync().awaitStopped()));
	}

}
