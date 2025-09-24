package com.demod.fbsr.entity;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.EntityType;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Layer;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bind.Bindings;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.SpriteDef;
import com.demod.fbsr.fp.FPAnimation;
import com.demod.fbsr.fp.FPVector;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;

@EntityType("fusion-reactor")
public class FusionReactorRendering extends EntityWithOwnerRendering {

	private List<FPAnimation> protoConnectionPictures;
	private List<FPVector> protoConnectionLocations;

	public static enum ConnectorType {
		END_PIPE, END_PLASMA, CONNECT_PIPE, CONNECT_PLASMA, CONNECT_BASE
	}

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		super.createRenderers(register, map, entity);

		boolean sideways = entity.getDirection().isHorizontal();

		Consumer<SpriteDef> connectorRegister = entity.spriteRegister(register, Layer.OBJECT);

		for (int i = 0; i < protoConnectionPictures.size(); i++) {
			FPAnimation fp = protoConnectionPictures.get(i);
			FPVector pos = protoConnectionLocations.get(i);
			List<Boolean> connections = map.getFusionConnections(entity.getPosition().add(MapPosition.convert(pos)));
			
			boolean modePlasma = (((i / 2) % 2) != 0) == sideways;
			ConnectorType type;
			if (connections.size() > 1) {
				boolean sameMode = connections.stream().collect(Collectors.groupingBy(k->k)).size() == 1;
				if (sameMode) {
					type = modePlasma ? ConnectorType.CONNECT_PLASMA : ConnectorType.CONNECT_PIPE;
				} else {
					type = ConnectorType.CONNECT_BASE;
				}
			} else {
				type = modePlasma ? ConnectorType.END_PLASMA : ConnectorType.END_PIPE;
			}
			fp.defineSprites(connectorRegister, type.ordinal());
		}
	}

	@Override
	public void defineEntity(Bindings bind, LuaTable lua) {
		super.defineEntity(bind, lua);
		
		LuaValue luaGraphicsSet = lua.get("graphics_set");
		bind.sprite4Way(luaGraphicsSet.get("structure"));

		bind.fluidBox(lua.get("input_fluid_box"));
		bind.fluidBox(lua.get("output_fluid_box"));
	}

	@Override
	public void initAtlas(Consumer<ImageDef> register) {
		super.initAtlas(register);

		protoConnectionPictures.forEach(fp -> {
			for (ConnectorType type : ConnectorType.values()) {
				fp.defineSprites(register, type.ordinal());
			}
		});
	}

	@Override
	public void initFromPrototype() {
		super.initFromPrototype();

		protoConnectionPictures = FPUtils.list(profile, prototype.lua().get("graphics_set").get("connections_graphics"), 
				(p, l) -> new FPAnimation(p, l.get("pictures")));

		protoConnectionLocations = FPUtils.list(prototype.lua().get("neighbour_connectable").get("connections"), 
				l -> new FPVector(l.get("location").get("position")));
	}

	@Override
	public void populateWorldMap(WorldMap map, MapEntity entity) {
		super.populateWorldMap(map, entity);

		boolean sideways = entity.getDirection().isHorizontal();

		for (int i = 0; i < protoConnectionLocations.size(); i++) {
			FPVector pos = protoConnectionLocations.get(i);
			boolean modePlasma = ((((i / 2) % 2) != 0) == sideways);
			List<Boolean> connections = map.getOrCreateFusionConnections(entity.getPosition().add(MapPosition.convert(pos)));
			connections.add(modePlasma);
		}
	}

}
