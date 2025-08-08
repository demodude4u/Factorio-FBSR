package com.demod.fbsr;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Semaphore;

import org.json.JSONException;
import org.json.JSONObject;

public class Config {
    public static class ConfigDiscord {
        public Boolean enabled;
        public String bot_token;
        public String reporting_user_id;
        public String reporting_channel_id;
        public String hosting_channel_id;
    }
    public static class ConfigWebAPI {
        public Boolean enabled;
        public String bind;
        public Integer port;
        public String local_storage;
    }
    public static class ConfigFBSR {
        public String profiles;
        public String build;
        public String assets;
    }
    public static class ConfigFactorio {
        public String install;
        public String executable;
    }
    public static class ConfigModPortal {
        public String username;
        public String password;
    }

    public ConfigDiscord discord;
    public ConfigWebAPI webapi;
    public ConfigFBSR fbsr;
    public ConfigFactorio factorio;
    public ConfigModPortal modportal;

    public static final File FILE = new File("config.json");

    // TODO read and write config file (using reflection and maintaining field order)
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

        Config config = new Config();
        
    }

    private static void unserialize(JSONObject json, Object obj) {
        for (var field : obj.getClass().getDeclaredFields()) {
            asdasfasfasfd
        }
    }
}
