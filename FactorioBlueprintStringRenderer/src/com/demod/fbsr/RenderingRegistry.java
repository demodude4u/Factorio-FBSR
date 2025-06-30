package com.demod.fbsr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RenderingRegistry {

    private final List<EntityRendererFactory> entityFactories = new ArrayList<>();
	private final List<TileRendererFactory> tileFactories = new ArrayList<>();
	private final Map<String, EntityRendererFactory> entityFactoryByName = new HashMap<>();
	private final Map<String, TileRendererFactory> tileFactoryByName = new HashMap<>();

	public void addEntity(EntityRendererFactory factory) {
		entityFactories.add(factory);
		entityFactoryByName.put(factory.getName(), factory);
	}

	public void addTile(TileRendererFactory factory) {
		tileFactories.add(factory);
		tileFactoryByName.put(factory.getName(), factory);
	}

	public List<EntityRendererFactory> getEntityFactories() {
		return entityFactories;
	}

	public List<TileRendererFactory> getTileFactories() {
		return tileFactories;
	}

	public Map<String, EntityRendererFactory> getEntityFactoryByNameMap() {
		return entityFactoryByName;
	}

	public Map<String, TileRendererFactory> getTileFactoryByNameMap() {
		return tileFactoryByName;
	}

	public Optional<EntityRendererFactory> lookupEntityFactoryByName(String name) {
		return Optional.ofNullable(entityFactoryByName.get(name));
	}

	public Optional<TileRendererFactory> lookupTileFactoryByName(String name) {
		return Optional.ofNullable(tileFactoryByName.get(name));
	}
}
