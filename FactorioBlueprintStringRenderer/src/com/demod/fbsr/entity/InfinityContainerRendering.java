package com.demod.fbsr.entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.json.JSONObject;

import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.prototype.ItemPrototype;
import com.demod.fbsr.BSUtils;
import com.demod.fbsr.Direction;
import com.demod.fbsr.Layer;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.bs.BSInfinitySettings;
import com.demod.fbsr.entity.InfinityContainerRendering.BSInfinityContainerEntity;

public class InfinityContainerRendering extends ContainerRendering<BSInfinityContainerEntity> {
	public static class BSInfinityContainerEntity extends BSEntity {
		public final Optional<BSInfinitySettings> infinitySettings;

		public BSInfinityContainerEntity(JSONObject json) {
			super(json);

			infinitySettings = BSUtils.opt(json, "infinity_settings", BSInfinitySettings::new);
		}
	}

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable,
			BSInfinityContainerEntity entity) {
		super.createRenderers(register, map, dataTable, entity);

		if (entity.infinitySettings.isPresent() && map.isAltMode()) {
			List<String> items = entity.infinitySettings.get().filters.stream().filter(bs -> bs.count > 0)
					.map(bs -> bs.name).collect(Collectors.toList());

			// TODO show double/quad icons if more than one
			if (!items.isEmpty()) {
				String itemName = items.get(0);
				Optional<ItemPrototype> optItem = dataTable.getItem(itemName);
				if (optItem.isPresent()) {
					Sprite spriteIcon = new Sprite();
					spriteIcon.image = FactorioData.getIcon(optItem.get());
					spriteIcon.source = new Rectangle(0, 0, spriteIcon.image.getWidth(), spriteIcon.image.getHeight());
					spriteIcon.bounds = new Rectangle2D.Double(-0.3, -0.3, 0.6, 0.6);

					Renderer delegate = RenderUtils.spriteRenderer(spriteIcon, entity, protoSelectionBox);
					register.accept(new Renderer(Layer.ENTITY_INFO_ICON, delegate.getBounds()) {
						@Override
						public void render(Graphics2D g) throws Exception {
							g.setColor(new Color(0, 0, 0, 128));
							g.fill(spriteIcon.bounds);
							delegate.render(g);
						}
					});
				}
			}
		}
	}

	@Override
	public void populateLogistics(WorldMap map, DataTable dataTable, BSInfinityContainerEntity entity) {
		Point2D.Double pos = entity.position.createPoint();

		if (entity.infinitySettings.isPresent()) {

			Set<String> outputs = entity.infinitySettings.get().filters.stream().filter(bs -> bs.count > 0)
					.map(bs -> bs.name).collect(Collectors.toSet());

			map.getOrCreateLogisticGridCell(Direction.NORTHEAST.offset(pos, 0.25)).setOutputs(Optional.of(outputs));
			map.getOrCreateLogisticGridCell(Direction.NORTHWEST.offset(pos, 0.25)).setOutputs(Optional.of(outputs));
			map.getOrCreateLogisticGridCell(Direction.SOUTHEAST.offset(pos, 0.25)).setOutputs(Optional.of(outputs));
			map.getOrCreateLogisticGridCell(Direction.SOUTHWEST.offset(pos, 0.25)).setOutputs(Optional.of(outputs));
		}
	}
}
