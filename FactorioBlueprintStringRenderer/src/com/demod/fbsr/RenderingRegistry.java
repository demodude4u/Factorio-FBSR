package com.demod.fbsr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RenderingRegistry {

    private static final List<EntityRendererFactory> entityFactories = new ArrayList<>();
	private static final List<TileRendererFactory> tileFactories = new ArrayList<>();
	private static final Map<String, EntityRendererFactory> entityFactoryByName = new HashMap<>();
	private static final Map<String, TileRendererFactory> tileFactoryByName = new HashMap<>();

}
