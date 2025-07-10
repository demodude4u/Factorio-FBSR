package com.demod.fbsr.cli;

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.json.JSONObject;

import com.demod.factorio.Config;
import com.demod.factorio.FactorioData;
import com.demod.factorio.Utils;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "config", description = "Configuration commands for FBSR")
public class CmdConfig {

    private static class PortalParams {
        @Option(names = "-portal-user", description = "Username for Factorio Mod Portal API", required = true) String username;
        @Option(names = "-portal-pass", description = "Password for Factorio Mod Portal API", required = true) String password;
    }

    @Command(name = "factorio", description = "Setup Factorio configuration")
    public void setupFactorio(
            @Option(names = "-install", description = "Path to Factorio installation", required = true) File folderInstall,
            @Option(names = "-profiles", description = "Path to profiles directory", defaultValue = "profiles") File folderProfiles,
            @Option(names = "-build", description = "Path to build directory", defaultValue = "build") File folderBuild,
            @ArgGroup(exclusive = false, multiplicity = "0..1") PortalParams portalParams
    ) {
        JSONObject jsonFactorio = new JSONObject();
        Utils.terribleHackToHaveOrderedJSONObject(jsonFactorio);
        jsonFactorio.put("install", folderInstall.getAbsolutePath());
        jsonFactorio.put("profiles", folderProfiles.getAbsolutePath());
        jsonFactorio.put("build", folderBuild.getAbsolutePath());

        if (portalParams != null) {
            jsonFactorio.put("portal", new JSONObject()
                    .put("username", portalParams.username)
                    .put("password", portalParams.password));
        }

        if (writeConfigFeature("factorio", jsonFactorio)) {
            System.out.println("Factorio configuration updated successfully.");
        } else {
            System.out.println("Failed to update Factorio configuration.");
        }
    }

    @Command(name = "discord", description = "Setup Discord configuration")
    public void setupDiscord(
            @Option(names = "-enabled", description = "Enable Discord Bot", defaultValue = "true", negatable = true) boolean enabled,
            @Option(names = "-token", description = "Discord Bot Token", required = true) String token,
            @Option(names = "-hosting", description = "Channel ID for hosting images and blueprints", required = true) String hostingChannelId,
            @Option(names = "-reporting-user", description = "User ID for reporting commands") Optional<String> reportingUserId,
            @Option(names = "-reporting-channel", description = "Channel ID for reporting commands") Optional<String> reportingChannelId
    ) {
        JSONObject jsonDiscord = new JSONObject();
        Utils.terribleHackToHaveOrderedJSONObject(jsonDiscord);
        jsonDiscord.put("enabled", enabled);
        jsonDiscord.put("bot_token", token);
        jsonDiscord.put("hosting_channel_id", hostingChannelId);
        reportingUserId.ifPresent(id -> jsonDiscord.put("reporting_user_id", id));
        reportingChannelId.ifPresent(id -> jsonDiscord.put("reporting_channel_id", id));

        if (writeConfigFeature("discord", jsonDiscord)) {
            System.out.println("Discord configuration updated successfully.");
        } else {
            System.out.println("Failed to update Discord configuration.");
        }
    }

    @Command(name = "webapi", description = "Setup Web API configuration")
    public void setupWebAPI(
            @Option(names = "-enabled", description = "Enable Web API", defaultValue = "true", negatable = true) boolean enabled,
            @Option(names = "-bind", description = "IP address to bind the Web API", defaultValue = "0.0.0.0") String bind,
            @Option(names = "-port", description = "Port for the Web API", defaultValue = "8080") int port,
            @Option(names = "-local-storage", description = "Path to local storage directory (optional)") Optional<String> localStorage
    ) {
        JSONObject jsonWebAPI = new JSONObject();
        Utils.terribleHackToHaveOrderedJSONObject(jsonWebAPI);
        jsonWebAPI.put("enabled", enabled);
        jsonWebAPI.put("bind", bind);
        jsonWebAPI.put("port", port);
        localStorage.ifPresent(path -> jsonWebAPI.put("local_storage", path));

        if (writeConfigFeature("webapi", jsonWebAPI)) {
            System.out.println("Web API configuration updated successfully.");
        } else {
            System.out.println("Failed to update Web API configuration.");
        }
    }

    @Command(name = "logging", description = "Setup Logging configuration")
    public void setupLogging(
            @Option(names = "-file", description = "Path to the log file", defaultValue = "log.txt") File file
    ) {
        JSONObject jsonLogging = new JSONObject();
        Utils.terribleHackToHaveOrderedJSONObject(jsonLogging);
        jsonLogging.put("file", file.getAbsolutePath());

        if (writeConfigFeature("logging", jsonLogging)) {
            System.out.println("Logging configuration updated successfully.");
        } else {
            System.out.println("Failed to update Logging configuration.");
        }
    }

    private enum EnableFeatureSelect {
        discord,
        webapi
    }

    @Command(name = "enable", description = "Enable features")
    public void enableFeature(
            @Parameters(arity = "1..*", description = "Features to enable (${COMPLETION-CANDIDATES})", paramLabel = "FEATURE") List<EnableFeatureSelect> features
    ) {
        for (EnableFeatureSelect feature : features) {
            Config.setPath(Config.getPath()); // Ensure we have the latest config
            
            JSONObject jsonFeature = Config.get().getJSONObject(feature.name());
            jsonFeature.put("enabled", true);

            if (writeConfigFeature(feature.name(), jsonFeature)) {
                System.out.println(feature.name() + " feature enabled successfully.");
            } else {
                System.out.println("Failed to enable " + feature.name() + " feature.");
            }
        }
    }

    @Command(name = "disable", description = "Disable features")
    public void disableFeature(
            @Parameters(arity = "1..*", description = "Features to disable (${COMPLETION-CANDIDATES})", paramLabel = "FEATURE") List<EnableFeatureSelect> features
    ) {
        for (EnableFeatureSelect feature : features) {
            Config.setPath(Config.getPath()); // Ensure we have the latest config
            
            JSONObject jsonFeature = Config.get().getJSONObject(feature.name());
            jsonFeature.put("enabled", false);

            if (writeConfigFeature(feature.name(), jsonFeature)) {
                System.out.println(feature.name() + " feature disabled successfully.");
            } else {
                System.out.println("Failed to disable " + feature.name() + " feature.");
            }
        }
    }

    @Command(name = "show", description = "Show current configuration")
    public void showConfig(
        @Option(names = "-reveal-sensitive", description = "Reveal sensitive information in the configuration", defaultValue = "false") boolean revealSensitive
    ) {
        JSONObject jsonConfig = Config.get();
        {
            JSONObject jsonDiscord = jsonConfig.optJSONObject("discord", new JSONObject());
            boolean enabled = jsonDiscord.optBoolean("enabled", false);
            if (enabled) {
                System.out.println("\n[DISCORD]");
                System.out.println("Discord Enabled: \t" + enabled);
                String botToken = jsonDiscord.getString("bot_token");
                System.out.println("Discord Bot Token: \t" + (revealSensitive ? botToken : "******" + botToken.substring(botToken.length() - 4)));
                String hostingChannelId = jsonDiscord.getString("hosting_channel_id");
                System.out.println("Hosting Channel ID: \t" + (revealSensitive ? hostingChannelId : "******" + hostingChannelId.substring(hostingChannelId.length() - 4)));
                if (jsonDiscord.has("reporting_user_id")) {
                    String reportingUserId = jsonDiscord.getString("reporting_user_id");
                    System.out.println("Reporting User ID: \t" + (revealSensitive ? reportingUserId : "******" + reportingUserId.substring(reportingUserId.length() - 4)));
                } else {
                    System.out.println("Reporting User ID: <NOT SET>");
                }
                if (jsonDiscord.has("reporting_channel_id")) {
                    String reportingChannelId = jsonDiscord.getString("reporting_channel_id");
                    System.out.println("Reporting Channel ID: \t" + (revealSensitive ? reportingChannelId : "******" + reportingChannelId.substring(reportingChannelId.length() - 4)));
                } else {
                    System.out.println("Reporting Channel ID: <NOT SET>");
                }
            }
        }
        {
            JSONObject jsonWebAPI = jsonConfig.optJSONObject("webapi", new JSONObject());
            boolean enabled = jsonWebAPI.optBoolean("enabled", false);
            if (enabled) {
                System.out.println("\n[WEB API]");
                System.out.println("Web API Enabled: \t" + enabled);
                System.out.println("Web API Bind Address: \t" + jsonWebAPI.getString("bind"));
                System.out.println("Web API Port: \t\t" + jsonWebAPI.getInt("port"));
                if (jsonWebAPI.has("local_storage")) {
                    System.out.println("Local Storage Path: \t" + jsonWebAPI.getString("local_storage"));
                } else {
                    System.out.println("Local Storage Path: \t<NOT SET>");
                }
            }
        }
        {
            JSONObject jsonFactorio = jsonConfig.getJSONObject("factorio");
            System.out.println("\n[FACTORIO]");
            if (jsonFactorio.has("install")) {
                System.out.println("Factorio Install Path: \t" + jsonFactorio.getString("install"));
            } else {
                System.out.println("Factorio Install Path: \t<NOT SET>");
            }
            System.out.println("Profiles Directory: \t" + jsonFactorio.getString("profiles"));
            System.out.println("Build Directory: \t" + jsonFactorio.getString("build"));
            if (jsonFactorio.has("portal")) {
                JSONObject portal = jsonFactorio.getJSONObject("portal");
                System.out.println("Mod Portal Username: \t" + portal.getString("username"));
                System.out.println("Mod Portal Password: \t" + (revealSensitive ? portal.getString("password") : "******"));
            } else {
                System.out.println("Mod Portal Username: \t<NOT SET>");
                System.out.println("Mod Portal Password: \t<NOT SET>");
            }
        }
        {
            JSONObject jsonLogging = jsonConfig.getJSONObject("logging");
            System.out.println("\n[LOGGING]");
            System.out.println("Log File Path: \t" + jsonLogging.getString("file"));
        }
    }

    @Command(name = "edit", description = "Edit configuration file directly")
    public void editConfig() {
        File configFile = new File(Config.getPath());
        if (!configFile.exists()) {
            System.out.println("Configuration file does not exist: " + Config.getPath());
            return;
        }

        try {
            Desktop desktop = Desktop.getDesktop();
            desktop.edit(configFile);
            System.out.println("Opened configuration file in editor: " + Config.getPath());
            System.out.println("When done editing, run the `config reload` command to apply changes.");
        } catch (IOException e) {
            System.out.println("Failed to open configuration file in editor: " + e.getMessage());
        }
    }

    @Command(name = "reload", description = "Reload configuration from file")
    public void reloadConfig() {
        Config.setPath(Config.getPath());
        System.out.println("Configuration reloaded from file: " + Config.getPath());
    }

    @Command(name = "find-factorio", description = "Attempt to automatically find and set the default Factorio installation")
    public void findDefaultFactorio() {
        
        List<String> searchDirs = new ArrayList<>(List.of(
            "C:\\Program Files (x86)\\Steam\\steamapps\\common\\Factorio",
            "C:\\Program Files\\Steam\\steamapps\\common\\Factorio",
            "/usr/games/factorio",
            "/usr/local/games/factorio",
            "/Applications/Factorio.app/Contents/Resources/app"
        ));
        for (File dir : new File(".").listFiles()) {
            if (dir.isDirectory() && !searchDirs.contains(dir.getAbsolutePath())) {
                searchDirs.add(dir.getAbsolutePath());
            }
        }
        List<String> foundPaths = new ArrayList<>();
        for (String dirPath : searchDirs) {
            File dir = new File(dirPath);
            if (dir.exists() && dir.isDirectory()) {
                File factorioExecutable = FactorioData.getFactorioExecutable(dir);
                if (factorioExecutable.exists()) {
                    foundPaths.add(dir.getAbsolutePath());
                }
            }
        }

        if (foundPaths.isEmpty()) {
            System.out.println("No Factorio installations found.");
            return;
        }

        String installPath;
        if (foundPaths.size() == 1) {
            installPath = foundPaths.get(0);
            System.out.println("Found Factorio installation: " + installPath);
        
        } else {
            System.out.println("Multiple Factorio installations found:");
            for (int i = 0; i < foundPaths.size(); i++) {
                System.out.println((i + 1) + ": " + foundPaths.get(i));
            }
            System.out.print("Select the number of the installation to use: ");
            try {
                int selection = Integer.parseInt(System.console().readLine());
                if (selection < 1 || selection > foundPaths.size()) {
                    System.out.println("Invalid selection.");
                    return;
                }
                installPath = foundPaths.get(selection - 1);
            } catch (Exception e) {
                System.out.println("Invalid input.");
                return;
            }
        }

        Config.setPath(Config.getPath()); // Ensure we have the latest config
        JSONObject jsonFactorio = Config.get().getJSONObject("factorio");
        jsonFactorio.put("install", installPath);

        if (writeConfigFeature("factorio", jsonFactorio)) {
            System.out.println("Factorio install updated successfully.");
        } else {
            System.out.println("Failed to update Factorio install.");
        }
    }

    private boolean writeConfigFeature(String feature, JSONObject jsonFeature) {
        JSONObject jsonConfig = Config.get();
        jsonConfig.put(feature, jsonFeature);
        File file = new File(Config.getPath());
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(jsonConfig.toString(2));
            Config.setPath(Config.getPath());//Reset cached config
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
