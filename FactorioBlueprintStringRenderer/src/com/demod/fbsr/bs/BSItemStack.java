package com.demod.fbsr.bs;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;

public class BSItemStack {
	public final BSSignalID id;
	public final List<BSItemStackItem> itemsInInventory;

	public BSItemStack(JSONObject json) {
		id = new BSSignalID(json.getJSONObject("id"));
		itemsInInventory = BSUtils.list(json.getJSONObject("items"), "in_inventory", BSItemStackItem::new);
	}

	public BSItemStack(String legacyId, int legacyCount) {
		id = new BSSignalID(legacyId);
		// TODO currently assuming modules
		itemsInInventory = IntStream.range(0, legacyCount).mapToObj(i -> new BSItemStackItem())
				.collect(Collectors.toList());
	}
}
