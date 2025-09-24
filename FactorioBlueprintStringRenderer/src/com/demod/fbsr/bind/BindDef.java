package com.demod.fbsr.bind;

import java.util.function.Consumer;

import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.LayeredSpriteDef;
import com.demod.fbsr.map.MapEntity;

public class BindDef extends BindConditional {
	public void defineLayeredSprites(Consumer<LayeredSpriteDef> consumer, MapEntity entity) {}
	
	public void initAtlas(Consumer<ImageDef> register) {}
}