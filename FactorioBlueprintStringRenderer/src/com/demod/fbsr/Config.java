package com.demod.fbsr;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.function.BiFunction;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.demod.factorio.Utils;

public class Config {
    public ConfigDiscord discord;
    public static class ConfigDiscord {
        public Boolean enabled;
        public String bot_token;
        public String reporting_user_id;
        public String reporting_channel_id;
        public String hosting_channel_id;
    }
    
    public ConfigWebAPI webapi;
    public static class ConfigWebAPI {
        public Boolean enabled;
        public String bind;
        public Integer port;
        public String local_storage;
    }

    public ConfigFBSR fbsr;
    public static class ConfigFBSR {
        public String profiles;
        public String build;
        public String assets;
    }

    public ConfigFactorio factorio;
    public static class ConfigFactorio {
        public String install;
        public String executable;
    }

    public ConfigModPortal modportal;
    public static class ConfigModPortal {
        public String username;
        public String password;
    }

    public static final File FILE = new File("config.json");

    public static synchronized Config load() {

        if (!FILE.exists()) {
            System.out.println("Config file not found!");
            return null;
        }

        JSONObject json;
        try {
            json = new JSONObject(Files.readString(FILE.toPath()));
        } catch (JSONException | IOException e) {
            System.out.println("Failed to load config file: " + e.getMessage());
            return null;
        }

        return unserialize(json, Config.class);
    }

    public static synchronized boolean save(Config config) {
        JSONObject json = serialize(config);
        if (json == null) {
            return false;
        }

        try {
            Files.writeString(FILE.toPath(), json.toString(4));
            return true;
        } catch (IOException e) {
            System.out.println("Failed to save config file: " + e.getMessage());
            return false;
        }
    }

    private static final Map<Class<?>, BiFunction<JSONObject, String, Object>> UNPACKERS_OBJ = Map.of(
        String.class, JSONObject::getString,
        Boolean.class, JSONObject::getBoolean,
        Integer.class, JSONObject::getInt,
        Double.class, JSONObject::getDouble,
        Long.class, JSONObject::getLong,
        BigDecimal.class, JSONObject::getBigDecimal,
        BigInteger.class, JSONObject::getBigInteger
    );
    private static final Map<Class<?>, BiFunction<JSONArray, Integer, Object>> UNPACKERS_ARR = Map.of(
        String.class, JSONArray::getString,
        Boolean.class, JSONArray::getBoolean,
        Integer.class, JSONArray::getInt,
        Double.class, JSONArray::getDouble,
        Long.class, JSONArray::getLong,
        BigDecimal.class, JSONArray::getBigDecimal,
        BigInteger.class, JSONArray::getBigInteger
    );

    private static <T> T unserialize(JSONObject json, Class<T> type) {
        try {
            T obj = type.getDeclaredConstructor().newInstance();
            for (var field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (!field.canAccess(obj)) {
                    continue;
                }
                if (!json.has(field.getName())) {
                    System.out.println("Config key '" + field.getName() + "' not found!");
                    continue;
                }
            
                if (json.isNull(field.getName())) {
                    field.set(obj, null);
                    continue;
                }

                if (UNPACKERS_OBJ.containsKey(field.getType())) {
                    Object value = UNPACKERS_OBJ.get(field.getType()).apply(json, field.getName());
                    field.set(obj, value);
                    continue;
                }

                if (field.getType().isArray()) {
                    JSONArray arrayJson = json.getJSONArray(field.getName());
                    Class<?> componentType = field.getType().getComponentType();
                    Object array = Array.newInstance(componentType, arrayJson.length());
                    for (int i = 0; i < arrayJson.length(); i++) {
                        Object element;
                        if (UNPACKERS_ARR.containsKey(componentType)) {
                            element = UNPACKERS_ARR.get(componentType).apply(arrayJson, i);
                        } else if (componentType.isPrimitive() || componentType == String.class) {
                            element = arrayJson.get(i);
                        } else {
                            element = unserialize(arrayJson.getJSONObject(i), componentType);
                        }
                        Array.set(array, i, element);
                    }
                    field.set(obj, array);
                    continue;
                }

                Object value = unserialize(json.getJSONObject(field.getName()), field.getType());
                field.set(obj, value);
            }

            return obj;
        } catch (Exception e) {
            System.out.println("Failed to read config: " + e.getMessage());
            return null;
        }
    }

    private static JSONObject serialize(Object obj) {
        try {
            JSONObject json = new JSONObject();
            Utils.terribleHackToHaveOrderedJSONObject(json);
            for (var field : obj.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (!field.canAccess(obj)) {
                    continue;
                }
                
                Object value = field.get(obj);
                if (value == null) {
                    json.put(field.getName(), JSONObject.NULL);
                    continue;
                }

                if (UNPACKERS_OBJ.containsKey(field.getType())) {
                    json.put(field.getName(), value);
                    continue;
                }

                if (field.getType().isArray()) {
                    JSONArray arrayJson = new JSONArray();
                    for (int i = 0; i < Array.getLength(value); i++) {
                        Object element = Array.get(value, i);

                        if (element == null) {
                            arrayJson.put(i, JSONObject.NULL);
                            continue;
                        }

                        if (UNPACKERS_ARR.containsKey(field.getType().getComponentType())) {
                            arrayJson.put(i, element);
                        } else {
                            arrayJson.put(serialize(element));
                        }
                    }

                    json.put(field.getName(), arrayJson);
                    continue;
                }

                json.put(field.getName(), serialize(value));
            }
            return json;
        } catch (Exception e) {
            System.out.println("Failed to write config: " + e.getMessage());
            return null;
        }
    }
}
