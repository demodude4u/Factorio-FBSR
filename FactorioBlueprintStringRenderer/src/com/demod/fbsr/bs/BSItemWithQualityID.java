package com.demod.fbsr.bs;

import java.util.Optional;

import org.json.JSONObject;

import com.demod.fbsr.BSUtils;

public class BSItemWithQualityID {
    public final String name;
	public final Optional<String> quality;

    public BSItemWithQualityID(JSONObject json) {
		name = json.getString("name");
		quality = BSUtils.optString(json, "quality");
    }

    public BSItemWithQualityID(String legacyId) {
		name = legacyId;
		quality = Optional.empty();
	}

    public BSItemWithQualityID(String name, Optional<String> quality) {
        this.name = name;
        this.quality = quality;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((quality == null) ? 0 : quality.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BSItemWithQualityID other = (BSItemWithQualityID) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (quality == null) {
            if (other.quality != null)
                return false;
        } else if (!quality.equals(other.quality))
            return false;
        return true;
    }

    public String formatted() {
        if (quality.isEmpty() || quality.get().equals("normal") || quality.get().trim().isEmpty()) {
            return name;
        } else {
            return String.format("%s (%s)", name, quality.get().substring(0, 1).toUpperCase() + quality.get().substring(1));
        }
    }
}
