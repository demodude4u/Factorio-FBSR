package com.demod.fbsr.cli;

import java.awt.Desktop;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.demod.fbsr.FactorioManager;
import com.demod.fbsr.Profile;
import com.google.common.collect.Multimap;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "profile", description = "Configure profiles for mods")
public class CmdProfile {

    private volatile Profile profile = null;

    @Command(name = "select", description = "Select a profile to work with in interactive mode")
    public void selectProfile(
            @Parameters(arity = "1", description = "Name of the profile to select") String name
    ) {
        Profile profile = Profile.listProfiles().stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElse(null);
        if (profile == null) {
            System.out.println("Profile not found: " + name);
        } else {
            System.out.println("Selected profile: " + name);
        }
        this.profile = profile;
    }

    private boolean checkOrSelectProfile(Optional<String> name) {
        if (name.isEmpty()) {
            if (this.profile == null) {
                System.out.println("No profile selected. Use 'profile select <name>' or add '-name <name>' to select a profile.");
                return false;
            }
            return true;

        } else {
            Profile profile = Profile.byName(name.get());
            if (!profile.isValid()) {
                System.out.println("Profile not found or invalid: " + name);
                return false;
            }
            this.profile = profile;
            return true;
        }
    }

    @Command(name = "new", description = "Create a new profile")
    public void createNew(
            @Option(names = "-name", required = true, description = "Name of the profile to create") String name,
            @Parameters(description = "List of mods to include in the profile") String[] mods
    ) {
        Profile profile = Profile.byName(name);
        if (profile.generateProfile(mods)) {
            this.profile = profile;
            System.out.println("Profile created successfully:" + profile.getName() + " (" + profile.getFolderProfile().getAbsolutePath() + ")");
        } else {
            System.out.println("Failed to create profile!");
        }
    }

    @Command(name = "default-renderings", description = "Generate default renderings for the specified profile (profile status must be at BUILD_DATA or READY)")
    public void generateDefaultRenderings(
            @Option(names = "-name", description = "Name of the profile") Optional<String> name
    ) {
        if (!checkOrSelectProfile(name)) {
            return;
        }

        if (profile.generateDefaultRenderingConfiguration()) {
            System.out.println("Default renderings generated successfully.");
        } else {
            System.out.println("Failed to generate default renderings. Ensure the profile is at BUILD_DATA or READY status.");
        }
    }

    @Command(name = "default-vanilla", description = "Generate default vanilla profile")
    public void generateDefaultVanillaProfile(
            @Option(names = "-force", description = "Force regeneration of the default vanilla profile, even if it already exists") boolean force
    ) {
        if (Profile.generateDefaultVanillaProfile(force)) {
            System.out.println("Default vanilla profile created successfully.");
        } else {
            System.out.println("Failed to create default vanilla profile.");
        }
    }

    @Command(name = "status", description = "Get the status of a profile")
    public void getProfileStatus(
            @Option(names = "-name", description = "Name of the profile") Optional<String> name,
            @Option(names = "-detailed", description = "Include detailed information about the profile") boolean detailed
    ) {
        if (!checkOrSelectProfile(name)) {
            return;
        }

        System.out.println();
        System.out.println(profile.getStateCode()+ " " + profile.getName() + " (" + profile.getStatus() + ")");
        
        if (detailed) {
            System.out.println();
            System.out.println("Folder: " + profile.getFolderProfile().getAbsolutePath());
            System.out.println("Build Folder: " + profile.getFolderBuild().getAbsolutePath());
            System.out.println();
            System.out.println("Manifest:        " + (profile.hasManifest() ? "Yes" : "No"));
            System.out.println("Mods Downloaded: " + (profile.hasDownloaded() ? "Yes" : "No"));
            System.out.println("Factorio Dump:   " + (profile.hasDump() ? "Yes" : "No"));
            System.out.println("Data Generated:  " + (profile.hasData() ? "Yes" : "No"));
        }

        System.out.println();
        switch (profile.getStatus()) {
            case BUILD_MANIFEST:
                System.out.println("Profile is in BUILD_MANIFEST status. Next step is to run command 'profile build-manifest'");
                break;
            case BUILD_DOWNLOAD:
                System.out.println("Profile is in BUILD_DOWNLOAD status. Next step is to run command 'profile build-download'");
                break;
            case BUILD_DUMP:
                System.out.println("Profile is in BUILD_DUMP status. Next step is to run command 'profile build-dump'");
                break;
            case BUILD_DATA:
                System.out.println("Profile is in BUILD_DATA status. Next step is to run command 'profile build-data'");
                break;
            case READY:
                System.out.println("Profile is in READY status. To run the bot, use command 'bot run'");
                break;
            case DISABLED:
                System.out.println("Profile is in DISABLED status. It will be ignored when running the bot. To enable this profile, use command 'profile enable'");
                break;
            case INVALID:
                System.out.println("Profile is in INVALID status. It cannot be used until fixed. The profile needs to have a profile.json configured. You can generate a new profile using the command 'profile new -name <name> <mod1> <mod2> <mod3> ...'");
                break;
            case NEED_FACTORIO_INSTALL:
                System.out.println("Profile is in NEED_FACTORIO_INSTALL status. It requires a Factorio installation configured in config.json to be set up in order to dump factorio data.");
                break;
            case NEED_MOD_PORTAL_API:
                System.out.println("Profile is in NEED_MOD_PORTAL_API status. It requires the Factorio Mod Portal API information to be configured in config.json in order to download mods.");
                break;
        }
    }

    @Command(name = "list", description = "List all profiles or mods")
    public void listProfiles(
            @Option(names = "-filter", description = "Filter profiles by name (partial)") String filter,
            @Option(names = "-detailed", description = "Include mods in the profile listing") boolean detailed
    ) {
        List<Profile> profiles = Profile.listProfiles();

        if (!profiles.stream().anyMatch(Profile::isVanilla)) {
            System.out.println("WARNING: No vanilla profile found. You need to create one using command 'profile default-vanilla'.");
        }

        if (profiles.isEmpty()) {
            System.out.println("No profiles found.");
            return;
        }
        System.out.println("Current profiles:");
        for (Profile profile : profiles) {
            if (filter != null && !profile.getName().contains(filter)) {
                continue;
            }
            System.out.println(" - " + profile.getStateCode()+ " " + profile.getName() + " (" + profile.getStatus() + ")");
            if (detailed && profile.hasManifest()) {
                List<String> mods = profile.listMods();
                for (String mod : mods) {
                    System.out.println("      > " + mod);
                }
            }
        }
    }

    @Command(name = "disable", description = "Disable a profile")
    public void disableProfile(
            @Option(names = "-name", description = "Name of the profile") Optional<String> name
    ) {
        if (!checkOrSelectProfile(name)) {
            return;
        }

        if (profile.setEnabled(false)) {
            System.out.println("Profile disabled successfully: " + profile.getName());
        } else {
            System.out.println("Failed to disable profile: " + profile.getName());
        }
    }

    @Command(name = "enable", description = "Enable a profile")
    public void enableProfile(
            @Option(names = "-name", description = "Name of the profile") Optional<String> name
    ) {
        if (!checkOrSelectProfile(name)) {
            return;
        }

        if (profile.setEnabled(true)) {
            System.out.println("Profile enabled successfully: " + profile.getName());
        } else {
            System.out.println("Failed to enable profile: " + profile.getName());
        }
    }

    @Command(name = "delete", description = "Delete a profile")
    public void deleteProfile(
            @Option(names = "-name", description = "Name of the profile") Optional<String> name,
            @Option(names = "-confirm", description = "Skip confirmation prompt") boolean confirm
    ) {
        if (!checkOrSelectProfile(name)) {
            return;
        }

        if (!confirm) {
            System.out.print("Type the profile name (" + profile.getName() + ") to confirm deletion: ");
            @SuppressWarnings("resource")
            String input = new java.util.Scanner(System.in).nextLine();
            if (!input.equals(profile.getName())) {
                System.out.println("Confirmation failed. Profile deletion aborted.");
                return;
            }
        }

        if (profile.delete()) {
            System.out.println("Profile deleted successfully: " + profile.getName());
            this.profile = null;
        } else {
            System.out.println("Failed to delete profile: " + profile.getName());
        }
    }

    @Command(name = "factorio", description = "Run Factorio with the specified profile")
    public void runFactorio(
            @Option(names = "-name", description = "Name of the profile") Optional<String> name
    ) {
        if (!checkOrSelectProfile(name)) {
            return;
        }

        if (!profile.runFactorio()) {
            System.out.println("Failed to run Factorio with profile: " + profile.getName());
        }
    }

    @Command(name = "explore", description = "Open file manager for the specified profile")
    public void explore(
            @Option(names = "-name", description = "Name of the profile") Optional<String> name,
            @Option(names = "-build", description = "Open the build folder instead of the profile folder") boolean build
    ) {
        if (!checkOrSelectProfile(name)) {
            return;
        }

        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(build ? profile.getFolderBuild() : profile.getFolderProfile());
            } catch (IOException e) {
                System.out.println("Failed to open profile in file manager: " + e.getMessage());
            }
        } else {
            System.out.println("Desktop is not supported. Cannot open profile in file manager.");
        }
    }

    @Command(name = "build-manifest", description = "Build the manifest for the specified profile")
    public void buildManifest(
            @Option(names = "-name", description = "Name of the profile") Optional<String> name,
            @Option(names = "-force", description = "Force regeneration of the manifest, even if it already exists") boolean force
    ) {
        if (!checkOrSelectProfile(name)) {
            return;
        }

        if (profile.buildManifest(force)) {
            System.out.println("Manifest built successfully for profile: " + profile.getName());
        } else {
            System.out.println("Failed to build manifest for profile: " + profile.getName());
        }
    }

    @Command(name = "build-download", description = "Download mods for the specified profile")
    public void buildDownloadMods(
            @Option(names = "-name", description = "Name of the profile") Optional<String> name,
            @Option(names = "-force", description = "Force redownload of mods, even if they already exist") boolean force
    ) {
        if (!checkOrSelectProfile(name)) {
            return;
        }

        if (profile.buildDownload(force)) {
            System.out.println("Mods downloaded successfully for profile: " + profile.getName());
        } else {
            System.out.println("Failed to download mods for profile: " + profile.getName());
        }
    }

    @Command(name = "build-dump", description = "Dump factorio data for the specified profile")
    public void buildDumpDataRaw(
            @Option(names = "-name", description = "Name of the profile") Optional<String> name,
            @Option(names = "-force", description = "Force regeneration of data.raw, even if it already exists") boolean force
    ) {
        if (!checkOrSelectProfile(name)) {
            return;
        }

        if (profile.buildDump(force)) {
            System.out.println("Factorio data dumped successfully for profile: " + profile.getName());
        } else {
            System.out.println("Failed to dump factorio data for profile: " + profile.getName());
        }
    }

    @Command(name = "build-data", description = "Generate data for the specified profile")
    public void buildGenerateData(
            @Option(names = "-name", description = "Name of the profile") Optional<String> name,
            @Option(names = "-force", description = "Force regeneration of data, even if it already exists") boolean force
    ) {
        if (!checkOrSelectProfile(name)) {
            return;
        }

        if (profile.buildData(force)) {
            System.out.println("Data generated successfully for profile: " + profile.getName());
        } else {
            System.out.println("Failed to generate data for profile: " + profile.getName());
        }
    }

    @Command(name = "clear-manifest", description = "Clear the manifest for the specified profile")
    public void clearManifest(
            @Option(names = "-name", description = "Name of the profile") Optional<String> name
    ) {
        if (!checkOrSelectProfile(name)) {
            return;
        }

        if (profile.clearManifest()) {
            System.out.println("Manifest cleared successfully for profile: " + profile.getName());
        } else {
            System.out.println("Failed to clear manifest for profile: " + profile.getName());
        }
    }

    @Command(name = "clear-download", description = "Clear all downloaded mods for the specified profile")
    public void clearDownload(
            @Option(names = "-name", description = "Name of the profile") Optional<String> name
    ) {
        if (!checkOrSelectProfile(name)) {
            return;
        }

        if (profile.clearAllDownloads()) {
            System.out.println("Downloaded mods cleared successfully for profile: " + profile.getName());
        } else {
            System.out.println("Failed to clear downloaded mods for profile: " + profile.getName());
        }
    }

    @Command(name = "clear-download-invalid", description = "Clear invalid downloaded mods for the specified profile")
    public void clearDownloadInvalid(
            @Option(names = "-name", description = "Name of the profile") Optional<String> name
    ) {
        if (!checkOrSelectProfile(name)) {
            return;
        }

        if (profile.clearInvalidDownloads()) {
            System.out.println("Invalid downloaded mods cleared successfully for profile: " + profile.getName());
        } else {
            System.out.println("Failed to clear invalid downloaded mods for profile: " + profile.getName());
        }
    }

    @Command(name = "clear-dump", description = "Clear dumped factorio data for the specified profile")
    public void clearDumpDataRaw(
            @Option(names = "-name", description = "Name of the profile") Optional<String> name
    ) {
        if (!checkOrSelectProfile(name)) {
            return;
        }

        if (profile.clearDump()) {
            System.out.println("Factorio data dump cleared successfully for profile: " + profile.getName());
        } else {
            System.out.println("Failed to clear factorio data dump for profile: " + profile.getName());
        }
    }

    @Command(name = "clear-data", description = "Clear generated data for the specified profile")
    public void clearGenerateData(
            @Option(names = "-name", description = "Name of the profile") Optional<String> name
    ) {
        if (!checkOrSelectProfile(name)) {
            return;
        }

        if (profile.clearData()) {
            System.out.println("Generated data cleared successfully for profile: " + profile.getName());
        } else {
            System.out.println("Failed to clear generated data for profile: " + profile.getName());
        }
    }
}
