package com.demod.fbsr.map;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.prototype.ItemPrototype;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.FactorioManager;
import com.demod.fbsr.Layer;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.BSItemStack;
import com.demod.fbsr.def.LayeredSpriteDef;
import com.demod.fbsr.def.SpriteDef;

public class MapEntity {
	public static class EntityModule {
		public final String name;
		public final Optional<String> quality;

		public EntityModule(String name, Optional<String> quality) {
			this.name = name;
			this.quality = quality;
		}
	}

	public static List<EntityModule> findModules(BSEntity entity) {

		List<EntityModule> modules = new ArrayList<>();

		for (BSItemStack itemStack : entity.items) {
			String itemName = itemStack.id.name;
			Optional<ItemPrototype> item = FactorioManager.lookupItemByName(itemName);
			if (item.isPresent() && item.get().getType().equals("module")) {
				for (int i = 0; i < itemStack.itemsInInventory.size(); i++) {
					modules.add(new EntityModule(itemName, itemStack.id.quality));
				}
			}
		}

		return modules;
	}

	private final BSEntity entity;

	private final EntityRendererFactory factory;
	private final MapPosition position;
	private final Direction direction;
	private final MapRect3D bounds;

	private final List<EntityModule> modules;

	public <E extends BSEntity> MapEntity(E entity, EntityRendererFactory factory) {
		this.entity = entity;
		this.factory = factory;

		position = entity.position.createPoint();
		direction = entity.direction;
		bounds = factory.getDrawBounds(this);
		modules = findModules(entity);
	}

	@SuppressWarnings("unchecked")
	public <E extends BSEntity> E fromBlueprint() {
		return (E) entity;
	}

	public MapRect3D getBounds() {
		return bounds;
	}

	public Direction getDirection() {
		return direction;
	}

	public EntityRendererFactory getFactory() {
		return factory;
	}

	public List<EntityModule> getModules() {
		return modules;
	}

	public MapPosition getPosition() {
		return position;
	}

	public Consumer<LayeredSpriteDef> spriteRegister(Consumer<MapRenderable> register) {
		return s -> register.accept(new MapSprite(s, position));
	}

	public Consumer<SpriteDef> spriteRegister(Consumer<MapRenderable> register, Layer layer) {
		return s -> register.accept(new MapSprite(s, layer, position));
	}

	public Consumer<SpriteDef> spriteRegister(Consumer<MapRenderable> register, Layer layer, MapPosition offset) {
		return s -> register.accept(new MapSprite(s, layer, position.add(offset)));
	}

	public Consumer<SpriteDef> spriteRegisterWithTintOverride(Consumer<MapRenderable> register, Layer layer,
			Color tint) {
		return s -> {
			if (s.applyRuntimeTint()) {
				register.accept(new MapTintOverrideSprite(s, layer, position, tint));
			} else {
				register.accept(new MapSprite(s, layer, position));
			}
		};
	}

	public Consumer<SpriteDef> spriteRegisterWithTintOverride(Consumer<MapRenderable> register, Layer layer,
			MapPosition offset, Color tint) {
		return s -> {
			if (s.applyRuntimeTint()) {
				register.accept(new MapTintOverrideSprite(s, layer, position.add(offset), tint));
			} else {
				register.accept(new MapSprite(s, layer, position.add(offset)));
			}
		};
	}
}
