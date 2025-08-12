package com.demod.fbsr.bs;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.json.JSONArray;
import org.json.JSONObject;

import com.demod.fbsr.map.MapVersion;

public class BSBuilder {

	public static BookBuilder book() {
		return new BookBuilder();
	}

	public static BlueprintBuilder blueprint() {
		return new BlueprintBuilder();
	}

	// Book builder
	public static class BookBuilder {
		private final List<JSONObject> children = new ArrayList<>();
		private Optional<String> label = Optional.empty();
		private Optional<String> description = Optional.empty();
		private Optional<Integer> activeIndex = Optional.empty();
		private MapVersion version = new MapVersion(2, 0, 0, 0);
		private final JSONArray icons = new JSONArray();

		public BookBuilder label(String label) {
			this.label = Optional.ofNullable(label);
			return this;
		}

		public BookBuilder description(String description) {
			this.description = Optional.ofNullable(description);
			return this;
		}

		public BookBuilder version(MapVersion version) {
			this.version = version;
			return this;
		}

		public BookBuilder activeIndex(int index) {
			this.activeIndex = Optional.of(index);
			return this;
		}

		public BookBuilder addBlueprint(BlueprintBuilder blueprint) {
			Objects.requireNonNull(blueprint, "blueprint");
			children.add(wrapBlueprint(blueprint.toBlueprintObject()));
			return this;
		}

		public BookBuilder addBlueprint(Consumer<BlueprintBuilder> config) {
			BlueprintBuilder b = new BlueprintBuilder();
			config.accept(b);
			return addBlueprint(b);
		}

		// Icons
		public BookBuilder icon(String type, String name, int index) {
			return icon(type, name, null, index);
		}
		public BookBuilder icon(String type, String name, String quality, int index) {
			JSONObject icon = new JSONObject();
			JSONObject signal = new JSONObject();
			signal.put("type", type);
			signal.put("name", name);
			if (quality != null && !quality.isBlank()) {
				signal.put("quality", quality);
			}
			icon.put("signal", signal);
			icon.put("index", index);
			icons.put(icon);
			return this;
		}
		public BookBuilder icon(BSSignalID signal, int index) {
			JSONObject icon = new JSONObject();
			JSONObject sig = new JSONObject();
			sig.put("type", signal.type);
			sig.put("name", signal.name);
			signal.quality.ifPresent(q -> sig.put("quality", q));
			icon.put("signal", sig);
			icon.put("index", index);
			icons.put(icon);
			return this;
		}
		public BookBuilder clearIcons() {
			while (icons.length() > 0) icons.remove(icons.length() - 1);
			return this;
		}

		public JSONObject toJson() {
			JSONObject book = new JSONObject();
			book.put("version", version.getSerialized());
			label.ifPresent(v -> book.put("label", v));
			description.ifPresent(v -> book.put("description", v));
			if (icons.length() > 0) {
				book.put("icons", icons);
			}
			JSONArray blueprints = new JSONArray();
			for (JSONObject child : children) {
				blueprints.put(child);
			}
			book.put("blueprints", blueprints);
			activeIndex.ifPresent(v -> book.put("active_index", v));

			JSONObject root = new JSONObject();
			root.put("blueprint_book", book);
			return root;
		}

		private static JSONObject wrapBlueprint(JSONObject blueprintObject) {
			JSONObject wrapped = new JSONObject();
			wrapped.put("blueprint", blueprintObject);
			return wrapped;
		}
	}

	// Blueprint builder
	public static class BlueprintBuilder {
		private Optional<String> label = Optional.empty();
		private Optional<String> description = Optional.empty();
		private MapVersion version = new MapVersion(2, 0, 0, 0);

		private final JSONArray entities = new JSONArray();
		private final JSONArray tiles = new JSONArray();
		private final JSONArray icons = new JSONArray();
		private int nextEntityNumber = 1;

		public BlueprintBuilder label(String label) {
			this.label = Optional.ofNullable(label);
			return this;
		}

		public BlueprintBuilder description(String description) {
			this.description = Optional.ofNullable(description);
			return this;
		}

		public BlueprintBuilder version(MapVersion version) {
			this.version = version;
			return this;
		}

		public EntityBuilder entity(String name, double x, double y) {
			return new EntityBuilder(this, name, x, y);
		}

		public BlueprintBuilder addEntity(String name, double x, double y) {
			return entity(name, x, y).done();
		}

		public BlueprintBuilder tile(String name, double x, double y) {
			JSONObject t = new JSONObject();
			JSONObject pos = new JSONObject();
			pos.put("x", x);
			pos.put("y", y);
			t.put("position", pos);
			t.put("name", name);
			tiles.put(t);
			return this;
		}

		// Icons
		public BlueprintBuilder icon(String type, String name, int index) {
			return icon(type, name, null, index);
		}
		public BlueprintBuilder icon(String type, String name, String quality, int index) {
			JSONObject icon = new JSONObject();
			JSONObject signal = new JSONObject();
			signal.put("type", type);
			signal.put("name", name);
			if (quality != null && !quality.isBlank()) {
				signal.put("quality", quality);
			}
			icon.put("signal", signal);
			icon.put("index", index);
			icons.put(icon);
			return this;
		}
		public BlueprintBuilder icon(BSSignalID signal, int index) {
			JSONObject icon = new JSONObject();
			JSONObject sig = new JSONObject();
			sig.put("type", signal.type);
			sig.put("name", signal.name);
			signal.quality.ifPresent(q -> sig.put("quality", q));
			icon.put("signal", sig);
			icon.put("index", index);
			icons.put(icon);
			return this;
		}
		public BlueprintBuilder clearIcons() {
			while (icons.length() > 0) icons.remove(icons.length() - 1);
			return this;
		}

		public JSONObject toJson() {
			JSONObject root = new JSONObject();
			root.put("blueprint", toBlueprintObject());
			return root;
		}

		private JSONObject toBlueprintObject() {
			JSONObject bp = new JSONObject();
			bp.put("version", version.getSerialized());
			label.ifPresent(v -> bp.put("label", v));
			description.ifPresent(v -> bp.put("description", v));
			if (icons.length() > 0) {
				bp.put("icons", icons);
			}

			if (entities.length() > 0) {
				bp.put("entities", entities);
			}
			if (tiles.length() > 0) {
				bp.put("tiles", tiles);
			}
			return bp;
		}

		private void addEntityObject(JSONObject e) {
			if (!e.has("entity_number")) {
				e.put("entity_number", nextEntityNumber++);
			}
			entities.put(e);
		}

		public static class EntityBuilder {
			private final BlueprintBuilder parent;
			private final JSONObject json = new JSONObject();

			private EntityBuilder(BlueprintBuilder parent, String name, double x, double y) {
				this.parent = parent;
				json.put("name", name);
				json.put("entity_number", parent.nextEntityNumber);
				JSONObject pos = new JSONObject();
				pos.put("x", x);
				pos.put("y", y);
				json.put("position", pos);
			}

			public EntityBuilder direction(int direction) {
				json.put("direction", direction);
				return this;
			}

			public EntityBuilder orientation(double orientation) {
				json.put("orientation", orientation);
				return this;
			}

			public EntityBuilder quality(String quality) {
				json.put("quality", quality);
				return this;
			}

			public BlueprintBuilder done() {
				parent.addEntityObject(json);
				parent.nextEntityNumber++;
				return parent;
			}
		}
	}
}
