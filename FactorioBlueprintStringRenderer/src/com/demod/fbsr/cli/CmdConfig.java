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
import org.rapidoid.config.Conf;
import org.rapidoid.data.JSON;

import com.demod.factorio.FactorioData;
import com.demod.factorio.Utils;
import com.demod.fbsr.Config;
import com.demod.fbsr.FactorioManager;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = " ", description = "Configuration commands for FBSR")
public class CmdConfig {

    private static class SetupFactorioInstall {
        @Option(names = "-install", description = "Path to Factorio installation", paramLabel = "<PATH>") Optional<String> folderInstall;
        @Option(names = "-find-install", description = "Automatically find Factorio installation in common directories") boolean findInstall;
        @Option(names = "-no-install", description = "Clear the Factorio installation path") boolean noInstall;
    }
    private static class SetupFactorioExecutable {
        @Option(names = "-executable", description = "Path to Factorio executable (optional)", paramLabel = "<PATH>") Optional<String> fileExecutable;
        @Option(names = "-auto-find-exec", description = "Automatically find the Factorio executable in the installation folder") boolean autoFindExec;
    }
    @Command(name = "cfg-factorio", description = "Modify Factorio configuration")
    public static void setupFactorio(
            @ArgGroup SetupFactorioInstall install,
            @ArgGroup SetupFactorioExecutable executable
    ) {
        Config config = Config.load();
        
        if (install != null) {
            if (install.findInstall) {
                Optional<String> defaultFactorioInstall = findDefaultFactorioInstall();
                if (defaultFactorioInstall.isPresent()) {
                    config.factorio.install = defaultFactorioInstall.get();
                    System.out.println("Factorio install path set to: " + defaultFactorioInstall.get());
                } else {
                    config.factorio.install = null;
                    System.out.println("Failed to find Factorio installation.");
                }
            } else if (install.folderInstall.isPresent()) {
                config.factorio.install = install.folderInstall.get();
                System.out.println("Factorio install path set to: " + install.folderInstall.get());
            } else if (install.noInstall) {
                config.factorio.install = null;
                System.out.println("Factorio install path cleared.");
            }
        }

        if (executable != null) {
            if (executable.fileExecutable.isPresent()) {
                config.factorio.executable = executable.fileExecutable.get();
                System.out.println("Factorio executable set to: " + executable.fileExecutable.get());
            } else if (executable.autoFindExec) {
                config.factorio.executable = null;
                System.out.println("Factorio executable will be automatically found in the installation folder.");
            }
        }

        if (Config.save(config)) {
            System.out.println("Factorio configuration updated successfully.");
        } else {
            System.out.println("Failed to update Factorio configuration.");
        }
    }

    @Command(name = "cfg-fbsr", description = "Modify FBSR configuration")
    public static void setupFBSR(
            @Option(names = "-profiles", description = "Path to profiles directory", paramLabel = "<PATH>") Optional<String> folderProfiles,
            @Option(names = "-build", description = "Path to build directory", paramLabel = "<PATH>") Optional<String> folderBuild,
            @Option(names = "-assets", description = "Path to assets directory", paramLabel = "<PATH>") Optional<String> folderAssets
    ) {
        Config config = Config.load();

        if (folderProfiles.isPresent()) {
            config.fbsr.profiles = folderProfiles.get();
            System.out.println("FBSR profiles directory set to: " + folderProfiles.get());
        }

        if (folderBuild.isPresent()) {
            config.fbsr.build = folderBuild.get();
            System.out.println("FBSR build directory set to: " + folderBuild.get());
        }

        if (folderAssets.isPresent()) {
            config.fbsr.assets = folderAssets.get();
            System.out.println("FBSR assets directory set to: " + folderAssets.get());
        }

        if (Config.save(config)) {
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
        Config config = Config.load();
        
        config.modportal.username = username;
        config.modportal.password = password;
        System.out.println("Mod Portal username set to: " + username);
        System.out.println("Mod Portal password set successfully.");

        if (Config.save(config)) {
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
        Config config = Config.load();

        if (enableDisable.enable) {
            config.discord.enabled = true;
            System.out.println("Discord feature enabled.");
        } else if (enableDisable.disable) {
            config.discord.enabled = false;
            System.out.println("Discord feature disabled.");
        }

        if (token.isPresent()) {
            config.discord.bot_token = token.get();
            System.out.println("Discord bot token set successfully.");
        }

        if (hostingChannelId.isPresent()) {
            config.discord.hosting_channel_id = hostingChannelId.get();
            System.out.println("Hosting channel ID set to: " + hostingChannelId.get());
        }
        
        if (reportingUser != null) {
            if (reportingUser.id.isPresent()) {
                config.discord.reporting_user_id = reportingUser.id.get();
                System.out.println("Reporting user ID set to: " + reportingUser.id.get());
            } else if (reportingUser.noReportingUserId) {
                config.discord.reporting_user_id = null;
                System.out.println("Reporting user ID is cleared.");
            }
        }

        if (reportingChannel != null) {
            if (reportingChannel.id.isPresent()) {
                config.discord.reporting_channel_id = reportingChannel.id.get();
                System.out.println("Reporting channel ID set to: " + reportingChannel.id.get());
            } else if (reportingChannel.noReportingChannelId) {
                config.discord.reporting_channel_id = null;
                System.out.println("Reporting channel ID is cleared.");
            }
        }

        if (Config.save(config)) {
            System.out.println("Discord configuration updated successfully.");
        } else {
            System.out.println("Failed to update Discord configuration.");
        }
    }

    private static class SetupWebAPILocalStorage {
        @Option(names = "-local-storage", description = "Path to local storage directory (optional)", paramLabel = "<PATH>") Optional<String> path;
        @Option(names = "-no-local-storage", description = "Do not use local storage") boolean noLocalStorage;
    }
    @Command(name = "cfg-webapi", description = "Modify Web API configuration")
    public static void setupWebAPI(
            @ArgGroup SetupEnableDisable enableDisable,
            @Option(names = "-bind", description = "IP address to bind the Web API", paramLabel = "<ADDRESS>") Optional<String> bind,
            @Option(names = "-port", description = "Port for the Web API", paramLabel = "<PORT>") Optional<Integer> port,
            @ArgGroup SetupWebAPILocalStorage localStorage
    ) {
        Config config = Config.load();

        if (enableDisable != null) {
            if (enableDisable.enable) {
                config.webapi.enabled = true;
                System.out.println("Web API feature enabled.");
            } else if (enableDisable.disable) {
                config.webapi.enabled = false;
                System.out.println("Web API feature disabled.");
            }
        }
        
        if (bind.isPresent()) {
            config.webapi.bind = bind.get();
            System.out.println("Web API bind address set to: " + bind.get());
        }

        if (port.isPresent()) {
            config.webapi.port = port.get();
            System.out.println("Web API port set to: " + port.get());
        }

        if (localStorage != null) {
            if (localStorage.path.isPresent()) {
                config.webapi.local_storage = localStorage.path.get();
                System.out.println("Web API local storage path set to: " + localStorage.path.get());
            } else if (localStorage.noLocalStorage) {
                config.webapi.local_storage = null;
                System.out.println("Web API local storage is disabled.");
            }
        }

        if (Config.save(config)) {
            System.out.println("Web API configuration updated successfully.");
        } else {
            System.out.println("Failed to update Web API configuration.");
        }
    }

    @Command(name = "cfg-show", description = "Show current configuration")
    public static void showConfig(
        @Option(names = "-reveal-sensitive", description = "Reveal sensitive information in the configuration", defaultValue = "false") boolean revealSensitive
    ) {
        Config config = Config.load();
        {
            boolean enabled = config.discord.enabled;
            if (enabled) {
                System.out.println("\n[DISCORD]");
                System.out.println("Discord Enabled: \t" + enabled);
                String botToken = config.discord.bot_token;
                System.out.println("Discord Bot Token: \t" + (revealSensitive ? botToken : "******" + botToken.substring(botToken.length() - 4)));
                String hostingChannelId = config.discord.hosting_channel_id;
                System.out.println("Hosting Channel ID: \t" + (revealSensitive ? hostingChannelId : "******" + hostingChannelId.substring(hostingChannelId.length() - 4)));
                if (config.discord.reporting_user_id != null) {
                    String reportingUserId = config.discord.reporting_user_id;
                    System.out.println("Reporting User ID: \t" + (revealSensitive ? reportingUserId : "******" + reportingUserId.substring(reportingUserId.length() - 4)));
                } else {
                    System.out.println("Reporting User ID: <NOT SET>");
                }
                if (config.discord.reporting_channel_id != null) {
                    String reportingChannelId = config.discord.reporting_channel_id;
                    System.out.println("Reporting Channel ID: \t" + (revealSensitive ? reportingChannelId : "******" + reportingChannelId.substring(reportingChannelId.length() - 4)));
                } else {
                    System.out.println("Reporting Channel ID: <NOT SET>");
                }
            }
        }
        {
            boolean enabled = config.webapi.enabled;
            if (enabled) {
                System.out.println("\n[WEB API]");
                System.out.println("Web API Enabled: \t" + enabled);
                System.out.println("Web API Bind Address: \t" + config.webapi.bind);
                System.out.println("Web API Port: \t\t" + config.webapi.port);
                if (config.webapi.local_storage != null) {
                    System.out.println("Local Storage Path: \t" + config.webapi.local_storage);
                } else {
                    System.out.println("Local Storage Path: \t<NOT SET>");
                }
            }
        }
        {
            System.out.println("\n[FBSR]");
            System.out.println("Profiles Directory: \t" + config.fbsr.profiles);
            System.out.println("Build Directory: \t" + config.fbsr.build);
            System.out.println("Assets Directory: \t" + config.fbsr.assets);
        }
        {
            System.out.println("\n[FACTORIO]");
            if (config.factorio.install != null) {
                System.out.println("Factorio Install Path: \t" + config.factorio.install);
            } else {
                System.out.println("Factorio Install Path: \t<NOT SET>");
            }
            if (config.factorio.executable != null) {
                System.out.println("Factorio Executable: \t" + config.factorio.executable);
            } else {
                System.out.println("Factorio Executable: \t<NOT SET>");
            }
        }
        {
            System.out.println("\n[MOD PORTAL]");
            if (config.modportal.username != null && config.modportal.password != null) {
                System.out.println("Mod Portal Username: \t" + config.modportal.username);
                System.out.println("Mod Portal Password: \t" + (revealSensitive ? config.modportal.password : "******"));
            } else {
                System.out.println("Mod Portal Username: \t<NOT SET>");
                System.out.println("Mod Portal Password: \t<NOT SET>");
            }
        }
    }

    @Command(name = "cfg-edit", description = "Edit configuration file in file editor")
    public static void editConfig() {
        if (!Config.FILE.exists()) {
            System.out.println("Configuration file does not exist: " + Config.FILE.getAbsolutePath());
            return;
        }

        try {
            Desktop desktop = Desktop.getDesktop();
            desktop.open(Config.FILE);
            System.out.println("Opened configuration file in editor: " + Config.FILE.getAbsolutePath());
            System.out.println("When done editing, run the `cfg-reload` command to apply changes.");
        } catch (IOException e) {
            System.out.println("Failed to open configuration file in editor: " + e.getMessage());
        }
    }

    @Command(name = "cfg-reload", description = "Reload configuration from file")
    public static void reloadConfig() {
        FactorioManager.reloadConfig();
        System.out.println("Configuration reloaded from file: " + Config.FILE.getAbsolutePath());
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
                searchDirs.add(dir.toPath().toAbsolutePath().normalize().toString());
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
}
