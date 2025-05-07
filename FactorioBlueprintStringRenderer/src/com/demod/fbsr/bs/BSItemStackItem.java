package com.demod.fbsr.bs;

import org.json.JSONObject;

public class BSItemStackItem {
	public final int inventory;
	public final int stack;
	public final int count;

	public BSItemStackItem(/* legacy */) {
		inventory = 0;
		stack = 0;
		count = 1;
	}

	public BSItemStackItem(JSONObject json) {
		inventory = json.getInt("inventory");
		stack = json.getInt("stack");
		count = json.optInt("count", 1);
	}
}
