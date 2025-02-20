package com.demod.fbsr.map;

import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.bs.BSEntity;

public class MapEntity {
	private final BSEntity entity;
	private final EntityRendererFactory<? extends BSEntity> factory;
	private final MapRect3D bounds;

	public <E extends BSEntity> MapEntity(E entity, EntityRendererFactory<E> factory) {
		this.entity = entity;
		this.factory = factory;
		bounds = factory.getDrawBounds(entity);
	}

	public BSEntity getEntity() {
		return entity;
	}

	@SuppressWarnings("unchecked")
	public <E extends BSEntity> EntityRendererFactory<E> getFactory() {
		return (EntityRendererFactory<E>) factory;
	}

	public MapRect3D getBounds() {
		return bounds;
	}
}
