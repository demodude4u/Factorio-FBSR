package com.demod.fbsr.bs;

import org.json.JSONObject;

public class BSItemStackItem {
	public final int inventory;
	public final int stack;

	public BSItemStackItem(JSONObject json) {
		inventory = json.getInt("inventory");
		stack = json.getInt("stack");
	}
}