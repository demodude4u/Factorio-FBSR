package com.demod.fbsr.fp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.luaj.vm2.LuaValue;

import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Sprite;
import com.google.common.collect.ImmutableList;

public class FPAnimationVariations {

	public final Optional<List<FPAnimationSheet>> sheets;
	public final Optional<List<FPAnimation>> animations;

	public FPAnimationVariations(LuaValue lua) {
		// XXX is there a better way to determine if this is an array?
		LuaValue luaFilenames = lua.get("filenames");
		LuaValue luaFilename = lua.get("filename");
		LuaValue luaSheet = lua.get("sheet");
		LuaValue luaSheets = lua.get("sheets");
		LuaValue luaLayers = lua.get("layers");
		if (luaFilenames.isnil() && luaFilename.isnil() && luaSheet.isnil() && luaSheets.isnil() && luaLayers.isnil()) {
			sheets = Optional.empty();
			animations = FPUtils.optList(lua, FPAnimation::new);
		} else if (luaSheet.isnil() && luaSheets.isnil()) {
			sheets = Optional.empty();
			animations = Optional.of(ImmutableList.of(new FPAnimation(lua)));
		} else if (!luaSheet.isnil()) {
			sheets = Optional.of(ImmutableList.of(new FPAnimationSheet(luaSheet)));
			animations = Optional.empty();
		} else {
			sheets = FPUtils.optList(luaSheets, FPAnimationSheet::new);
			animations = Optional.empty();
		}
	}

	public void createSprites(Consumer<Sprite> consumer, int variation, int frame) {
		if (sheets.isPresent()) {
			// Not sure if variation per sheet or acting as layers
			sheets.get().forEach(s -> s.createSprites(consumer, variation, frame));
		} else if (animations.isPresent()) {
			animations.get().get(variation).createSprites(consumer, frame);
		}
	}

	public List<Sprite> createSprites(int variation, int frame) {
		List<Sprite> ret = new ArrayList<>();
		createSprites(ret::add, variation, frame);
		return ret;
	}
}
