package com.demod.fbsr.map;

import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.prototype.ItemPrototype;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.FactorioManager;
import com.demod.fbsr.Layer;
import com.demod.fbsr.LayeredSpriteDef;
import com.demod.fbsr.SpriteDef;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.BSItemStack;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;

public class MapEntity {
	private final BSEntity entity;
	private final EntityRendererFactory factory;

	private final MapPosition position;
	private final Direction direction;
	private final MapRect3D bounds;
	private final Multiset<String> modules;

	public <E extends BSEntity> MapEntity(E entity, EntityRendererFactory factory) {
		this.entity = entity;
		this.factory = factory;

		position = entity.position.createPoint();
		direction = entity.direction;
		bounds = factory.getDrawBounds(this);
		modules = findModules(entity);
	}

	public MapRect3D getBounds() {
		return bounds;
	}

	@SuppressWarnings("unchecked")
	public <E extends BSEntity> E fromBlueprint() {
		return (E) entity;
	}

	public EntityRendererFactory getFactory() {
		return factory;
	}

	public MapPosition getPosition() {
		return position;
	}

	public Direction getDirection() {
		return direction;
	}

	public Multiset<String> getModules() {
		return modules;
	}

	public static Multiset<String> findModules(BSEntity entity) {

		Multiset<String> modules = LinkedHashMultiset.create();

		for (BSItemStack itemStack : entity.items) {
			String itemName = itemStack.id.name;
			Optional<ItemPrototype> item = FactorioManager.lookupItemByName(itemName);
			if (item.isPresent() && item.get().getType().equals("module")) {
				modules.add(itemName, itemStack.itemsInInventory.size());
			}
		}

		return modules;
	}

	public Consumer<SpriteDef> spriteRegister(Consumer<MapRenderable> register, Layer layer) {
		return s -> register.accept(new MapSprite(s, layer, position));
	}

	public Consumer<LayeredSpriteDef> spriteRegister(Consumer<MapRenderable> register) {
		return s -> register.accept(new MapSprite(s, position));
	}
}
