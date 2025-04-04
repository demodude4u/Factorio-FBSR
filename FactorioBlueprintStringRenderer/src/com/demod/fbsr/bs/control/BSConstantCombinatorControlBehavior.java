package com.demod.fbsr.bs.control;

import java.util.List;
import java.util.OptionalInt;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.bs.BSFilter;
import com.google.common.collect.ImmutableList;

public class BSConstantCombinatorControlBehavior {

	public static class BSConstantCombinatorControlBehaviorSection {
		public final OptionalInt index;
		public final List<BSFilter> filters;

		public BSConstantCombinatorControlBehaviorSection(JSONObject json) {
			index = BSUtils.optInt(json, "index");
			filters = BSUtils.list(json, "filters", BSFilter::new);
		}
	}

	public final List<BSConstantCombinatorControlBehaviorSection> sections;

	public BSConstantCombinatorControlBehavior(JSONObject json) {
		if (json.has("sections")) {
			JSONObject jsonSections = json.getJSONObject("sections");
			sections = BSUtils.list(jsonSections, "sections", BSConstantCombinatorControlBehaviorSection::new);
		} else {
			sections = ImmutableList.of();
		}
	}
}
