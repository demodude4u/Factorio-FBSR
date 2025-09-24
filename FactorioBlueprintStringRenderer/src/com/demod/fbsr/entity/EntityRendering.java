package com.demod.fbsr.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.demod.factorio.fakelua.LuaTable;
import com.demod.fbsr.Direction;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.Layer;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.WirePoint;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.WirePoint.WireColor;
import com.demod.fbsr.bind.BindCircuitConnector;
import com.demod.fbsr.bind.BindDef;
import com.demod.fbsr.bind.BindFluidBox;
import com.demod.fbsr.bind.BindHeatBuffer;
import com.demod.fbsr.bind.Bindings;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.LayeredSpriteDef;
import com.demod.fbsr.def.SpriteDef;
import com.demod.fbsr.fp.FPCircuitConnectorDefinition;
import com.demod.fbsr.fp.FPFluidBox;
import com.demod.fbsr.fp.FPHeatBuffer;
import com.demod.fbsr.fp.FPHeatConnection;
import com.demod.fbsr.fp.FPPipeConnectionDefinition;
import com.demod.fbsr.fp.FPWireConnectionPoint;
import com.demod.fbsr.map.MapEntity;
import com.demod.fbsr.map.MapPosition;
import com.demod.fbsr.map.MapRenderable;
import com.demod.fbsr.map.MapSprite;

public abstract class EntityRendering extends EntityRendererFactory {

	private Bindings bindings;

	@Override
	public void createRenderers(Consumer<MapRenderable> register, WorldMap map, MapEntity entity) {
		Consumer<LayeredSpriteDef> entityRegister = entity.spriteRegister(register);

		for (BindDef bindDef : bindings.getDefs()) {
			if (!bindDef.test(map, entity)) {
				continue;
			}

			bindDef.defineLayeredSprites(entityRegister, entity);
		}

		Direction dir = entity.getDirection();
		for (BindFluidBox bindFluidBox : bindings.getFluidBoxes()) {
			if (!bindFluidBox.test(map, entity)) {
				continue;
			}
			
			for (FPFluidBox fluidBox : bindFluidBox.getFluidBoxes()) {
				if (fluidBox.pipeCovers.isPresent() || fluidBox.pipePicture.isPresent()) {
					for (FPPipeConnectionDefinition conn : fluidBox.pipeConnections) {
						if (!conn.connectionType.equals("normal") || conn.position.isEmpty() || conn.direction.isEmpty()) {
							continue;
						}
						
						Direction connDir = conn.direction.get();
						MapPosition connPos = MapPosition.convert(conn.position.get());
	
						Direction facing = connDir.rotate(dir);
						MapPosition point = facing.offset(dir.rotate(connPos).add(entity.getPosition()), 1);
						Consumer<SpriteDef> pointRegister = s -> register.accept(new MapSprite(s, Layer.OBJECT, point));
	
						if (fluidBox.pipePicture.isPresent()) {
							fluidBox.pipePicture.get().defineSprites(pointRegister, facing);
						}
	
						if (fluidBox.pipeCovers.isPresent() && !map.isPipe(point, facing)) {
							fluidBox.pipeCovers.get().defineSprites(pointRegister, facing);
						}
					}
				}
			}
		}

		for (BindHeatBuffer bindHeatBuffer : bindings.getHeatBuffers()) {
			if (!bindHeatBuffer.test(map, entity)) {
				continue;
			}

			FPHeatBuffer heatBuffer = bindHeatBuffer.getHeatBuffer();
			if (heatBuffer.pipeCovers.isPresent()) {
				for (FPHeatConnection conn : heatBuffer.connections) {
					Direction connDir = conn.direction;
					MapPosition connPos = MapPosition.convert(conn.position);

					Direction facing = connDir.rotate(dir);
					MapPosition point = facing.offset(dir.rotate(connPos).add(entity.getPosition()), 1);
					Consumer<SpriteDef> pointRegister = s -> register.accept(new MapSprite(s, Layer.OBJECT, point));

					if (heatBuffer.pipeCovers.isPresent() && !map.isPipe(point, facing)) {
						heatBuffer.pipeCovers.get().defineSprites(pointRegister, facing);
					}
				}
			}
		}
	}

	public abstract void defineEntity(Bindings bind, LuaTable lua);

	@Override
	public void initAtlas(Consumer<ImageDef> register) {

		for (BindDef bindDef : bindings.getDefs()) {
			bindDef.initAtlas(register);
		}

		for (BindFluidBox bindFluidBox : bindings.getFluidBoxes()) {
			for (FPFluidBox fluidBox : bindFluidBox.getFluidBoxes()) {
				if (fluidBox.pipeCovers.isPresent() || fluidBox.pipePicture.isPresent()) {
					if (fluidBox.pipeConnections.stream().anyMatch(c -> c.connectionType.equals("normal"))) {
						fluidBox.pipePicture.ifPresent(fp -> fp.getDefs(register));
						fluidBox.pipeCovers.ifPresent(fp -> fp.getDefs(register));
					}
				}
			}
		}

		for (BindHeatBuffer bindHeatBuffer : bindings.getHeatBuffers()) {
			bindHeatBuffer.getHeatBuffer().pipeCovers.ifPresent(fp -> fp.getDefs(register));
		}

		for (BindCircuitConnector bindCircuitConnector : bindings.getCircuitConnectors()) {
			for (FPCircuitConnectorDefinition circuitConnector : bindCircuitConnector.getDefinitions()) {
				circuitConnector.sprites.ifPresent(sprites -> {
					sprites.connectorMain.ifPresent(fp -> fp.defineSprites(register));
					sprites.connectorShadow.ifPresent(fp -> fp.defineSprites(register));
					sprites.wirePins.ifPresent(fp -> fp.defineSprites(register));
					sprites.wirePinsShadow.ifPresent(fp -> fp.defineSprites(register));
				});
			}
		}
	}

	@Override
	public void initFromPrototype() {
		Bindings bindings = new Bindings(profile);
		defineEntity(bindings, prototype.lua());
		this.bindings = bindings;
	}

	@Override
	public void createWireConnector(Consumer<MapRenderable> register, BiConsumer<Integer, WirePoint> registerWirePoint, MapEntity entity, List<MapEntity> wired, WorldMap map) {
		for (BindCircuitConnector bindCircuitConnector : bindings.getCircuitConnectors()) {
			if (!bindCircuitConnector.test(map, entity)) {
				continue;
			}

			FPCircuitConnectorDefinition circuitConnector = RenderUtils.pickDirectional(
					bindCircuitConnector.getDefinitions(),
					entity);

			MapPosition pos = entity.getPosition();
			Consumer<SpriteDef> entityRegister = entity.spriteRegister(register, Layer.OBJECT);

			circuitConnector.sprites.ifPresent(sprites -> {
				sprites.connectorMain.ifPresent(fp -> fp.defineSprites(entityRegister));
				sprites.connectorShadow.ifPresent(fp -> fp.defineSprites(entityRegister));
				sprites.wirePins.ifPresent(fp -> fp.defineSprites(entityRegister));
				sprites.wirePinsShadow.ifPresent(fp -> fp.defineSprites(entityRegister));
			});

			if (circuitConnector.points.isPresent()) {
				FPWireConnectionPoint cp = circuitConnector.points.get();
				if (cp.wire.red.isPresent()) {
					registerWirePoint.accept(1, WirePoint.fromConnectionPoint(WireColor.RED, cp, entity));
				}
				if (cp.wire.green.isPresent()) {
					registerWirePoint.accept(2, WirePoint.fromConnectionPoint(WireColor.GREEN, cp, entity));
				}
				//Should copper default to 5?
			}
		}
	}

	@Override
	public void populateWorldMap(WorldMap map, MapEntity entity) {
		Direction dir = entity.getDirection();
		
		for (BindFluidBox bindFluidBox : bindings.getFluidBoxes()) {
			if (!bindFluidBox.test(map, entity)) {
				continue;
			}
			
			for (FPFluidBox fluidBox : bindFluidBox.getFluidBoxes()) {
				for (FPPipeConnectionDefinition conn : fluidBox.pipeConnections) {
					if (conn.direction.isPresent() && conn.position.isPresent()) {
						Direction facing = conn.direction.get().rotate(dir);
						MapPosition pos = dir.rotate(MapPosition.convert(conn.position.get())).add(entity.getPosition());
						// TODO use flow direction for pipe arrow logistics
						map.setPipe(pos, facing);
					}
				}
			}
		}

		for (BindHeatBuffer bindHeatBuffer : bindings.getHeatBuffers()) {
			if (!bindHeatBuffer.test(map, entity)) {
				continue;
			}

			for (FPHeatConnection conn : bindHeatBuffer.getHeatBuffer().connections) {
				Direction facing = conn.direction.rotate(dir);
				MapPosition pos = dir.rotate(MapPosition.convert(conn.position)).add(entity.getPosition());
				map.setHeatPipe(pos, facing);
			}
		}
	}

}
