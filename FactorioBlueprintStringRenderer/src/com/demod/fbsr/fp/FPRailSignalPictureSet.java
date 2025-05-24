package com.demod.fbsr.fp;

import java.util.List;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.ModsProfile;

public class FPRailSignalPictureSet {
	public final FPRotatedAnimation structure;
	public final FPRailSignalStaticSpriteLayer railPiece;
	public final List<Integer> structureAlignToAnimationIndex;
	public final List<FPVector> selectionBoxShift;

	public FPRailSignalPictureSet(ModsProfile profile, LuaValue lua) {
		structure = new FPRotatedAnimation(profile, lua.get("structure"));
		railPiece = new FPRailSignalStaticSpriteLayer(profile, lua.get("rail_piece"));
		structureAlignToAnimationIndex = FPUtils.list(lua.get("structure_align_to_animation_index"),
				LuaValue::toint);
		selectionBoxShift = FPUtils.list(lua.get("selection_box_shift"), FPVector::new);
	}
}