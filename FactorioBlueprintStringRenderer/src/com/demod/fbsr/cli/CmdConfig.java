package com.demod.fbsr.cli;

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.FocusManager;

import org.json.JSONObject;
import org.rapidoid.data.JSON;

import com.demod.factorio.Config;
import com.demod.factorio.FactorioData;
import com.demod.factorio.Utils;
import com.demod.fbsr.FactorioManager;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = " ", description = "Configuration commands for FBSR")
public class CmdConfig {

    private static class SetupFactorioInstall {
        @Option(names = "-install", description = "Path to Factorio installation", paramLabel = "<PATH>") Optional<File> folderInstall;
        @Option(names = "-find-install", description = "Automatically find Factorio installation in common directories") boolean findInstall;
    }
    private static class SetupFactorioExecutable {
        @Option(names = "-executable", description = "Path to Factorio executable (optional)", paramLabel = "<PATH>") Optional<File> fileExecutable;
        @Option(names = "-auto-find-exec", description = "Automatically find the Factorio executable in the installation folder") boolean autoFindExec;
    }
    @Command(name = "cfg-factorio", description = "Modify Factorio configuration")
    public static void setupFactorio(
            @ArgGroup SetupFactorioInstall install,
            @ArgGroup SetupFactorioExecutable executable
    ) {
        JSONObject jsonOld = readConfigFeature("factorio");
        JSONObject json = new JSONObject();
        Utils.terribleHackToHaveOrderedJSONObject(json);
        
        if (install.findInstall) {
            Optional<String> defaultFactorioInstall = findDefaultFactorioInstall();
            if (defaultFactorioInstall.isPresent()) {
                json.put("install", defaultFactorioInstall.get());
                System.out.println("Factorio install path set to: " + defaultFactorioInstall.get());
            } else {
                System.out.println("Failed to find Factorio installation.");
                json.put("install", jsonOld.opt("install"));
            }
        } else if (install.folderInstall.isPresent()) {
            json.put("install", install.folderInstall.get().getAbsolutePath());
            System.out.println("Factorio install path set to: " + install.folderInstall.get().getAbsolutePath());
        } else {
            json.put("install", jsonOld.opt("install"));
        }

        if (executable.fileExecutable.isPresent()) {
            json.put("executable", executable.fileExecutable.get().getAbsolutePath());
            System.out.println("Factorio executable set to: " + executable.fileExecutable.get().getAbsolutePath());
        } else if (executable.autoFindExec) {
            json.put("executable", JSONObject.NULL);
            System.out.println("Factorio executable will be automatically found in the installation folder.");
        } else {
            json.put("executable", jsonOld.opt("executable"));
        }

        if (writeConfigFeature("factorio", json)) {
            System.out.println("Factorio configuration updated successfully.");
        } else {
            System.out.println("Failed to update Factorio configuration.");
        }
    }

    @Command(name = "cfg-fbsr", description = "Modify FBSR configuration")
    public static void setupFBSR(
            @Option(names = "-profiles", description = "Path to profiles directory", paramLabel = "<PATH>") Optional<File> folderProfiles,
            @Option(names = "-build", description = "Path to build directory", paramLabel = "<PATH>") Optional<File> folderBuild,
            @Option(names = "-assets", description = "Path to assets directory", paramLabel = "<PATH>") Optional<File> folderAssets
    ) {
        JSONObject jsonOld = readConfigFeature("fbsr");
        JSONObject json = new JSONObject();
        Utils.terribleHackToHaveOrderedJSONObject(json);

        if (folderProfiles.isPresent()) {
            folderProfiles.ifPresent(file -> json.put("profiles", file.getAbsolutePath()));
            System.out.println("FBSR profiles directory set to: " + folderProfiles.get().getAbsolutePath());
        }
        else {
            json.put("profiles", jsonOld.opt("profiles"));
        }

        if (folderBuild.isPresent()) {
            folderBuild.ifPresent(file -> json.put("build", file.getAbsolutePath()));
            System.out.println("FBSR build directory set to: " + folderBuild.get().getAbsolutePath());
        }
        else {
            json.put("build", jsonOld.opt("build"));
        }

        if (folderAssets.isPresent()) {
            folderAssets.ifPresent(file -> json.put("assets", file.getAbsolutePath()));
            System.out.println("FBSR assets directory set to: " + folderAssets.get().getAbsolutePath());
        }
        else {
            json.put("assets", jsonOld.opt("assets"));
        }

        if (writeConfigFeature("fbsr", json)) {
            System.out.println("FBSR configuration updated successfully.");
        } else {
            System.out.println("Failed to update FBSR configuration.");
        }
    }

    @Command(name = "cfg-modportal", description = "Modify Mod Portal configuration")
    public static void setupModPortal(
            @Option(names = "-username", description = "Username for Factorio Mod Portal API", required = true, paramLabel = "<USERNAME>") String username,
            @Option(names = "-password", description = "Password for Factorio Mod Portal API", interactive = true, paramLabel = "<PASSWORD>") String password
    ) {
        JSONObject json = new JSONObject();
        Utils.terribleHackToHaveOrderedJSONObject(json);
        json.put("username", username);
        json.put("password", password);
        System.out.println("Mod Portal username set to: " + username);
        System.out.println("Mod Portal password set successfully.");

        if (writeConfigFeature("modportal", json)) {
            System.out.println("Mod Portal configuration updated successfully.");
        } else {
            System.out.println("Failed to update Mod Portal configuration.");
        }
    }

    private static class SetupEnableDisable {
        @Option(names = "-enable", description = "Enable the feature") boolean enable;
        @Option(names = "-disable", description = "Disable the feature") boolean disable;
    }

    private static class SetupDiscordReportingUser {
        @Option(names = "-reporting-user", description = "User ID for reporting commands", paramLabel = "<ID>") Optional<String> id;
        @Option(names = "-no-reporting-user", description = "Do not set a reporting user ID") boolean noReportingUserId;
    }
    private static class SetupDiscordReportingChannel {
        @Option(names = "-reporting-channel", description = "Channel ID for reporting commands", paramLabel = "<ID>") Optional<String> id;
        @Option(names = "-no-reporting-channel", description = "Do not set a reporting channel ID") boolean noReportingChannelId;
    }
    @Command(name = "cfg-discord", description = "Modify Discord configuration")
    public static void setupDiscord(
            @ArgGroup SetupEnableDisable enableDisable,
            @Option(names = "-token", description = "Discord Bot Token", paramLabel = "<TOKEN>") Optional<String> token,
            @Option(names = "-hosting", description = "Channel ID for hosting images and blueprints", paramLabel = "<ID>") Optional<String> hostingChannelId,
            @ArgGroup SetupDiscordReportingUser reportingUser,
            @ArgGroup SetupDiscordReportingChannel reportingChannel
    ) {
        JSONObject jsonOld = readConfigFeature("discord");
        JSONObject json = new JSONObject();
        Utils.terribleHackToHaveOrderedJSONObject(json);
        
        if (enableDisable.enable) {
            json.put("enabled", true);
            System.out.println("Discord feature enabled.");
        } else if (enableDisable.disable) {
            json.put("enabled", false);
            System.out.println("Discord feature disabled.");
        } else {
            json.put("enabled", jsonOld.opt("enabled"));
        }

        if (token.isPresent()) {
            json.put("bot_token", token.get());
            System.out.println("Discord bot token set successfully.");
        } else {
            json.put("bot_token", jsonOld.opt("bot_token"));
        }

        if (hostingChannelId.isPresent()) {
            json.put("hosting_channel_id", hostingChannelId.get());
            System.out.println("Hosting channel ID set to: " + hostingChannelId.get());
        } else {
            json.put("hosting_channel_id", jsonOld.opt("hosting_channel_id"));
        }
        
        if (reportingUser.id.isPresent()) {
            json.put("reporting_user_id", reportingUser.id.get());
            System.out.println("Reporting user ID set to: " + reportingUser.id.get());
        } else if (reportingUser.noReportingUserId) {
            json.remove("reporting_user_id");
            System.out.println("Reporting user ID is cleared.");
        } else {
            json.put("reporting_user_id", jsonOld.opt("reporting_user_id"));
        }

        if (reportingChannel.id.isPresent()) {
            json.put("reporting_channel_id", reportingChannel.id.get());
            System.out.println("Reporting channel ID set to: " + reportingChannel.id.get());
        } else if (reportingChannel.noReportingChannelId) {
            json.remove("reporting_channel_id");
            System.out.println("Reporting channel ID is cleared.");
        } else {
            json.put("reporting_channel_id", jsonOld.opt("reporting_channel_id"));
        }

        if (writeConfigFeature("discord", json)) {
            System.out.println("Discord configuration updated successfully.");
        } else {
            System.out.println("Failed to update Discord configuration.");
        }
    }

    private static class SetupWebAPILocalStorage {
        @Option(names = "-local-storage", description = "Path to local storage directory (optional)", paramLabel = "<PATH>") Optional<File> path;
        @Option(names = "-no-local-storage", description = "Do not use local storage") boolean noLocalStorage;
    }
    @Command(name = "cfg-webapi", description = "Modify Web API configuration")
    public static void setupWebAPI(
            @ArgGroup SetupEnableDisable enableDisable,
            @Option(names = "-bind", description = "IP address to bind the Web API", paramLabel = "<ADDRESS>") Optional<String> bind,
            @Option(names = "-port", description = "Port for the Web API", paramLabel = "<PORT>") Optional<Integer> port,
            @ArgGroup SetupWebAPILocalStorage localStorage
    ) {
        JSONObject jsonOld = readConfigFeature("webapi");
        JSONObject json = new JSONObject();
        Utils.terribleHackToHaveOrderedJSONObject(json);

        if (enableDisable.enable) {
            json.put("enabled", true);
            System.out.println("Web API feature enabled.");
        } else if (enableDisable.disable) {
            json.put("enabled", false);
            System.out.println("Web API feature disabled.");
        } else {
            json.put("enabled", jsonOld.opt("enabled"));
        }
        
        if (bind.isPresent()) {
            json.put("bind", bind.get());
            System.out.println("Web API bind address set to: " + bind.get());
        } else {
            json.put("bind", jsonOld.opt("bind"));
        }

        if (port.isPresent()) {
            json.put("port", port.get());
            System.out.println("Web API port set to: " + port.get());
        } else {
            json.put("port", jsonOld.opt("port"));
        }

        if (localStorage.path.isPresent()) {
            json.put("local_storage", localStorage.path.get().getAbsolutePath());
            System.out.println("Web API local storage path set to: " + localStorage.path.get().getAbsolutePath());
        } else if (localStorage.noLocalStorage) {
            json.remove("local_storage");
            System.out.println("Web API local storage is disabled.");
        } else {
            json.put("local_storage", jsonOld.opt("local_storage"));
        }

        if (writeConfigFeature("webapi", json)) {
            System.out.println("Web API configuration updated successfully.");
        } else {
            System.out.println("Failed to update Web API configuration.");
        }
    }

    @Command(name = "cfg-show", description = "Show current configuration")
    public static void showConfig(
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
                if (!jsonDiscord.isNull("reporting_user_id")) {
                    String reportingUserId = jsonDiscord.getString("reporting_user_id");
                    System.out.println("Reporting User ID: \t" + (revealSensitive ? reportingUserId : "******" + reportingUserId.substring(reportingUserId.length() - 4)));
                } else {
                    System.out.println("Reporting User ID: <NOT SET>");
                }
                if (!jsonDiscord.isNull("reporting_channel_id")) {
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
                if (!jsonWebAPI.isNull("local_storage")) {
                    System.out.println("Local Storage Path: \t" + jsonWebAPI.getString("local_storage"));
                } else {
                    System.out.println("Local Storage Path: \t<NOT SET>");
                }
            }
        }
        {
            JSONObject jsonFactorio = jsonConfig.getJSONObject("factorio");
            System.out.println("\n[FACTORIO]");
            if (!jsonFactorio.isNull("install")) {
                System.out.println("Factorio Install Path: \t" + jsonFactorio.getString("install"));
            } else {
                System.out.println("Factorio Install Path: \t<NOT SET>");
            }
            System.out.println("Profiles Directory: \t" + jsonFactorio.optString("profiles", "profiles"));
            System.out.println("Build Directory: \t" + jsonFactorio.optString("build", "build"));
            System.out.println("Assets Directory: \t" + jsonFactorio.optString("assets", "assets"));
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

    @Command(name = "cfg-edit", description = "Edit configuration file in file editor")
    public static void editConfig() {
        File configFile = new File(Config.getPath());
        if (!configFile.exists()) {
            System.out.println("Configuration file does not exist: " + Config.getPath());
            return;
        }

        try {
            Desktop desktop = Desktop.getDesktop();
            desktop.open(configFile);
            System.out.println("Opened configuration file in editor: " + Config.getPath());
            System.out.println("When done editing, run the `cfg-reload` command to apply changes.");
        } catch (IOException e) {
            System.out.println("Failed to open configuration file in editor: " + e.getMessage());
        }
    }

    @Command(name = "cfg-reload", description = "Reload configuration from file")
    public static void reloadConfig() {
        FactorioManager.reloadConfig();
        System.out.println("Configuration reloaded from file: " + Config.getPath());
    }

    private static Optional<String> findDefaultFactorioInstall() {
        List<String> searchDirs = new ArrayList<>(List.of(
            "C:\\Program Files (x86)\\Steam\\steamapps\\common\\Factorio",
            "C:\\Program Files\\Steam\\steamapps\\common\\Factorio",
            "/usr/games/factorio",
            "/usr/local/games/factorio",
            "/Applications/Factorio.app/Contents/Resources/app",
            System.getProperty("user.home") + "/Library/Application Support/Steam/steamapps/common/Factorio"
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
            return Optional.empty();
        }

        String installPath;
        if (foundPaths.size() == 1) {
            installPath = foundPaths.get(0);
        
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
                    return Optional.empty();
                }
                installPath = foundPaths.get(selection - 1);
            } catch (Exception e) {
                System.out.println("Invalid input.");
                return Optional.empty();
            }
        }
        
        return Optional.of(installPath);
    }

    private static JSONObject readConfigFeature(String feature) {
        JSONObject jsonConfig = Config.get();
        if (jsonConfig.has(feature)) {
            return jsonConfig.getJSONObject(feature);
        } else {
            System.out.println("Feature '" + feature + "' does not exist in the configuration.");
            return null;
        }
    }

    private static boolean writeConfigFeature(String feature, JSONObject jsonFeature) {
        JSONObject jsonConfig = Config.get();
        jsonConfig.put(feature, jsonFeature);
        File file = new File(Config.getPath());
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(jsonConfig.toString(2));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        FactorioManager.reloadConfig();
        return true;
    }
}
