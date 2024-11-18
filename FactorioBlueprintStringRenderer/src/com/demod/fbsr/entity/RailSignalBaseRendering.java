package com.demod.fbsr.entity;

import org.luaj.vm2.LuaValue;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.fp.FPAnimation;
import com.demod.fbsr.fp.FPRotatedAnimation;

public abstract class RailSignalBaseRendering extends EntityRendererFactory {

	protected FPAnimation protoGroundRailPieceSprites;
	protected FPRotatedAnimation protoGroundStructure;

	// TODO elevated versions

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		LuaValue groundLua = prototype.lua().get("ground_picture_set");
		protoGroundRailPieceSprites = new FPAnimation(groundLua.get("rail_piece").get("sprites"));
		protoGroundStructure = new FPRotatedAnimation(groundLua.get("structure"));
	}

}
