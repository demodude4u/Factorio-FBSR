package com.demod.fbsr.bs.entity;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.demod.fbsr.BSUtils;
import com.demod.fbsr.bs.BSEntity;
import com.demod.fbsr.legacy.LegacyBlueprintEntity;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

public class BSAsteroidCollectorEntity extends BSEntity {

    public final List<String> chunkFilter;

    public BSAsteroidCollectorEntity(JSONObject json) {
        super(json);

        if (json.has("chunk-filter")) {
            JSONArray jsonChunkFilter = json.getJSONArray("chunk-filter");
		    Builder<String> builder = ImmutableList.builder();
            for (int i = 0; i < jsonChunkFilter.length(); i++) {
                Object chunk = jsonChunkFilter.get(i);
                if (chunk instanceof String) {
                    builder.add((String) chunk);
                }
                if (chunk instanceof JSONObject) {
                    JSONObject chunkObj = (JSONObject) chunk;
                    if (chunkObj.has("name")) {
                        builder.add(chunkObj.getString("name"));
                    }
                }
            }
            chunkFilter = builder.build();
            
        } else {
            chunkFilter = ImmutableList.of();
        }
    }

    public BSAsteroidCollectorEntity(LegacyBlueprintEntity legacy) {
        super(legacy);

        chunkFilter = ImmutableList.of();
    }
}
