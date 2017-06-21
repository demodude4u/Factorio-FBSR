package com.demod.fbsr.app;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceFinder {
	private static final Map<Class<?>, Object> registry = new ConcurrentHashMap<>();

	public static void addService(Object service) {
		registry.put(service.getClass(), service);
	}

	@SuppressWarnings("unchecked")
	public static <T> Optional<T> findService(Class<T> clazz) {
		return Optional.ofNullable((T) registry.get(clazz));
	}

	public static void removeService(Object service) {
		registry.remove(service.getClass());
	}
}
