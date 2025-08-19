package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.demod.factorio.fakelua.LuaValue;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Profile;
import com.demod.fbsr.def.ImageDef;
import com.demod.fbsr.def.SpriteDef;
import com.google.common.collect.ImmutableList;

public class FPAnimationVariations {

	public final Optional<List<FPAnimationSheet>> sheets;
	public final Optional<List<FPAnimation>> animations;

	public FPAnimationVariations(Profile profile, LuaValue lua) {
		// XXX is there a better way to determine if this is an array?
		LuaValue luaFilenames = lua.get("filenames");
		LuaValue luaFilename = lua.get("filename");
		LuaValue luaSheet = lua.get("sheet");
		LuaValue luaSheets = lua.get("sheets");
		LuaValue luaLayers = lua.get("layers");
		if (luaFilenames.isnil() && luaFilename.isnil() && luaSheet.isnil() && luaSheets.isnil() && luaLayers.isnil()) {
			sheets = Optional.empty();
			animations = FPUtils.optList(profile, lua, FPAnimation::new);
		} else if (luaSheet.isnil() && luaSheets.isnil()) {
			sheets = Optional.empty();
			animations = Optional.of(ImmutableList.of(new FPAnimation(profile, lua)));
		} else if (!luaSheet.isnil()) {
			sheets = Optional.of(ImmutableList.of(new FPAnimationSheet(profile, luaSheet)));
			animations = Optional.empty();
		} else {
			sheets = FPUtils.optList(profile, luaSheets, FPAnimationSheet::new);
			animations = Optional.empty();
		}
	}

	public void defineSprites(Consumer<? super SpriteDef> consumer, int variation, int frame) {
		if (sheets.isPresent()) {
			sheets.get().forEach(s -> s.defineSprites(consumer, variation, frame));
		} else if (animations.isPresent()) {
			animations.get().get(variation).defineSprites(consumer, frame);
		}
	}

	public List<SpriteDef> defineSprites(int variation, int frame) {
		List<SpriteDef> ret = new ArrayList<>();
		defineSprites(ret::add, variation, frame);
		return ret;
	}

	public void getDefs(Consumer<ImageDef> register, int frame) {
		if (sheets.isPresent()) {
			sheets.get().forEach(s -> s.getDefs(register, frame));
		} else if (animations.isPresent()) {
			animations.get().forEach(fp -> fp.defineSprites(register, frame));
		}
	}
}
