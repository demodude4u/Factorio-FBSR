package com.demod.fbsr.app;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginFinder {
	private static final Logger LOGGER = LoggerFactory.getLogger(PluginFinder.class);

	public interface Plugin {
		public void run();
	}

	@SuppressWarnings("deprecation")
	public static List<Plugin> loadPlugins() {
		List<Plugin> ret = new ArrayList<>();
		File folder = new File("plugins");

		if (folder.exists()) {
			for (File file : folder.listFiles()) {
				if (file.getName().endsWith(".jar")) {
					try {
						@SuppressWarnings("resource")
						URLClassLoader classLoader = new URLClassLoader(new URL[] { file.toURI().toURL() });
						ret.add((Plugin) classLoader.loadClass("Plugin").newInstance());
						LOGGER.info("PLUGIN LOADED: {}", file.getName());
					} catch (Exception e) {
						LOGGER.error("FAILED TO LOAD PLUGIN: {}", file.getName(), e);
					}
				}
			}
		}

		return ret;
	}
}
