package com.demod.fbsr.app;

import java.util.LinkedHashSet;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import com.demod.factorio.Config;
import com.google.common.util.concurrent.AbstractScheduledService;

public class WatchdogService extends AbstractScheduledService {

	public static interface WatchdogReporter {
		public void notifyInactive(String label);

		public void notifyReactive(String label);
	}

	private final LinkedHashSet<String> known = new LinkedHashSet<>();
	private final LinkedHashSet<String> active = new LinkedHashSet<>();
	private final LinkedHashSet<String> alarmed = new LinkedHashSet<>();
	private JSONObject configJson;

	public synchronized void notifyActive(String label) {
		known.add(label);
		active.add(label);
		if (alarmed.remove(label)) {
			System.out.println("WATCHDOG: " + label + " is now active again!");
			ServiceFinder.findService(WatchdogReporter.class).ifPresent(reporter -> {
				reporter.notifyReactive(label);
			});
		}
	}

	public synchronized void notifyKnown(String label) {
		if (known.add(label)) {
			active.add(label);
		}
	}

	@Override
	protected synchronized void runOneIteration() {
		for (String label : known) {
			if (!active.contains(label) && !alarmed.contains(label)) {
				alarmed.add(label);
				System.out.println("WATCHDOG: " + label + " has gone inactive!");
				ServiceFinder.findService(WatchdogReporter.class).ifPresent(reporter -> {
					reporter.notifyInactive(label);
				});
			}
		}
		active.clear();
	}

	@Override
	protected Scheduler scheduler() {
		return Scheduler.newFixedDelaySchedule(0, configJson.getInt("interval_minutes"), TimeUnit.MINUTES);
	}

	@Override
	protected void shutDown() {
		ServiceFinder.removeService(this);
	}

	@Override
	protected void startUp() {
		ServiceFinder.addService(this);

		configJson = Config.get().getJSONObject("watchdog");
	}

}
