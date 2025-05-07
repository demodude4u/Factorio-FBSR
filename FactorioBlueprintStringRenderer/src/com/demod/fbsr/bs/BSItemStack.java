package com.demod.fbsr.bs;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;

public class BSItemStack {
	public final BSItemWithQualityID id;
	public final List<BSItemStackItem> itemsInInventory;
	
	private int totalCount;

	public BSItemStack(JSONObject json) {
		id = new BSItemWithQualityID(json.getJSONObject("id"));
		itemsInInventory = BSUtils.list(json.getJSONObject("items"), "in_inventory", BSItemStackItem::new);
		totalCount = itemsInInventory.stream().mapToInt(i -> i.count).sum();
	}

	public BSItemStack(String legacyId, int legacyCount) {
		id = new BSItemWithQualityID(legacyId);
		itemsInInventory = IntStream.range(0, legacyCount).mapToObj(i -> new BSItemStackItem())
				.collect(Collectors.toList());
	}

	public int getTotalCount() {
		return totalCount;
	}
}
