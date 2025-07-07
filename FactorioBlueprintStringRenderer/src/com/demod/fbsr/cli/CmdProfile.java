package com.demod.fbsr.cli;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONException;
import org.json.JSONObject;

import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.prototype.DataPrototype;
import com.demod.fbsr.FactorioManager;
import com.demod.fbsr.Profile;
import com.demod.fbsr.Profile.ProfileStatus;
import com.demod.fbsr.RenderRequest.Debug;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "profile", description = "Configure profiles for mods")
public class CmdProfile {

    private volatile Profile profile = null;

    @Command(name = "select", description = "Select a profile to work with in interactive mode")
    public void selectProfile(
            @Parameters(arity = "1", description = "Name of the profile to select", paramLabel = "PROFILE") String name
    ) {
        Profile profile = Profile.listProfiles().stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElse(null);
        if (profile == null) {
            System.out.println("Profile not found: " + name);
        } else {
            System.out.println();
            System.out.println("Selected profile: " + name);

            getProfileStatus(Optional.of(profile.getName()), false);
        }
        this.profile = profile;
    }

    private boolean checkOrSelectProfile(Optional<String> name) {
        if (name.isEmpty()) {
            if (this.profile == null) {
                System.out.println("No profile selected. Use 'profile select <name>' or insert <name> into command to select a profile.");
                return false;
            }

        } else {
            Profile profile = Profile.byName(name.get());
            if (!profile.isValid()) {
                System.out.println("Profile not found or invalid: " + name);
                return false;
            }
            this.profile = profile;
        }

        System.out.println("Selected profile: " + this.profile.getName());
        return true;
    }

    @Command(name = "new", description = "Create a new profile")
    public void createNew(
            @Parameters(arity = "1", description = "Name of the profile to select", paramLabel = "PROFILE") String name,
            @Parameters(description = "List of mods to include in the profile", paramLabel = "MODS") String[] mods
    ) {
        Profile profile = Profile.byName(name);
        if (profile.generateProfile(mods)) {
            this.profile = profile;
            System.out.println("Profile created successfully:" + profile.getName() + " (" + profile.getFolderProfile().getAbsolutePath() + ")");
            System.out.println("To generate a new rendering table, run the command 'profile default-renderings " + profile.getName() + "'");

            if (!profile.updateMods()) {
                System.out.println("Failed to update mods for profile: " + profile.getName());
            }
        } else {
            System.out.println("Failed to create profile!");
        }
    }

    @Command(name = "default-renderings", description = "Generate default renderings for the specified profile (profile status must be at BUILD_DATA or READY)")
    public void generateDefaultRenderings(
            @Parameters(arity = "0..1", description = "Name of the profile", paramLabel = "PROFILE") Optional<String> name    
    ) {
        if (!checkOrSelectProfile(name)) {
            return;
        }

        if (!profile.buildManifest(true)) {
            System.out.println("Failed to build manifest for profile: " + profile.getName());
            return;
        }

        if (!profile.buildDownload(true)) {
            System.out.println("Failed to download mods for profile: " + profile.getName());
            return;
        }

        if (!profile.buildDump(true)) {
            System.out.println("Failed to dump factorio data for profile: " + profile.getName());
            return;
        }

        if (profile.generateDefaultRenderingConfiguration()) {
            System.out.println("Default renderings generated successfully. You can finish building the profile by running the command 'profile build-data " + profile.getName() + "'");
        } else {
            System.out.println("Failed to generate default renderings. Ensure the profile is at BUILD_DATA or READY status.");
        }
    }

    @Command(name = "default-vanilla", description = "Generate default vanilla profile")
    public void generateDefaultVanillaProfile(
            @Option(names = "-force", description = "Force regeneration of the default vanilla profile, even if it already exists") boolean force
    ) {
        if (Profile.generateDefaultVanillaProfile(force)) {
            System.out.println("Default vanilla profile created successfully. You can build the profile by running the command 'profile build vanilla'");
        } else {
            System.out.println("Failed to create default vanilla profile.");
        }
    }

    @Command(name = "status", description = "Get the status of a profile")
    public void getProfileStatus(
            @Parameters(arity = "0..1", description = "Name of the profile", paramLabel = "PROFILE") Optional<String> name,
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
                System.out.println("Profile is in BUILD_MANIFEST status. Next step is to run command 'profile build-manifest' or 'profile build'");
                break;
            case BUILD_DOWNLOAD:
                System.out.println("Profile is in BUILD_DOWNLOAD status. Next step is to run command 'profile build-download' or 'profile build'");
                break;
            case BUILD_DUMP:
                System.out.println("Profile is in BUILD_DUMP status. Next step is to run command 'profile build-dump' or 'profile build'");
                break;
            case BUILD_DATA:
                System.out.println("Profile is in BUILD_DATA status. Next step is to run command 'profile build-data' or 'profile build'");
                break;
            case READY:
                System.out.println("Profile is in READY status. To run the bot, use command 'bot run'");
                break;
            case DISABLED:
                System.out.println("Profile is in DISABLED status. It will be ignored when running the bot. To enable this profile, use command 'profile enable'");
                break;
            case INVALID:
                System.out.println("Profile is in INVALID status. It cannot be used until fixed. The profile needs to have a profile.json configured. You can generate a new profile using the command 'profile new <name> <mod1> <mod2> <mod3> ...'");
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

        if (profiles.isEmpty()) {
            System.out.println("No profiles found.");
            return;
        }
        System.out.println("Current profiles:");
        for (Profile profile : profiles) {
            if (filter != null && !profile.getName().contains(filter)) {
                continue;
            }
            System.out.println(profile.getStateCode()+ " " + profile.getName() + " (" + profile.getStatus() + ")" + (profile.hasVersionMismatch() ? " [VERSION MISMATCH]" : ""));
            if (detailed) {
                List<String> mods = profile.listMods();
                for (String mod : mods) {
                    System.out.println(" > " + mod);
                }
            }
        }

        if (!profiles.stream().anyMatch(Profile::isVanilla)) {
            System.out.println();
            System.out.println("WARNING: No vanilla profile found. You need to create one using command 'profile default-vanilla'.");
        }
    }

    @Command(name = "disable", description = "Disable a profile")
    public void disableProfile(
            @Parameters(arity = "0..1", description = "Name of the profile", paramLabel = "PROFILE") Optional<String> name,
            @Option(names = "-all", description = "Disable all profiles") boolean all
    ) {
        if (all) {
            for (Profile profile : Profile.listProfiles()) {
                if (!profile.isEnabled()) {
                    continue;
                }

                if (profile.setEnabled(false)) {
                    System.out.println("Profile disabled successfully: " + profile.getName());
                } else {
                    System.out.println("Failed to disable profile: " + profile.getName());
                }
            }
            return;
        }

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
            @Parameters(arity = "0..1", description = "Name of the profile", paramLabel = "PROFILE") Optional<String> name,
            @Option(names = "-all", description = "Enable all profiles") boolean all
    ) {
        if (all) {
            for (Profile profile : Profile.listProfiles()) {
                if (profile.isEnabled()) {
                    continue;
                }

                if (profile.setEnabled(true)) {
                    System.out.println("Profile enabled successfully: " + profile.getName());
                } else {
                    System.out.println("Failed to enable profile: " + profile.getName());
                }
            }
            return;
        }

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
            @Parameters(arity = "0..1", description = "Name of the profile", paramLabel = "PROFILE") Optional<String> name,
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
            @Parameters(arity = "0..1", description = "Name of the profile", paramLabel = "PROFILE") Optional<String> name
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
            @Parameters(arity = "0..1", description = "Name of the profile", paramLabel = "PROFILE") Optional<String> name,
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

    @Command(name = "edit", description = "Open the profile configuration file in the default editor")
    public void editProfile(
            @Parameters(arity = "0..1", description = "Name of the profile", paramLabel = "PROFILE") Optional<String> name
    ) {
        if (!checkOrSelectProfile(name)) {
            return;
        }

        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(profile.getFileProfile());
            } catch (IOException e) {
                System.out.println("Failed to open profile in editor: " + e.getMessage());
            }
        } else {
            System.out.println("Desktop is not supported. Cannot open profile in editor.");
        }
    }

    @Command(name = "update-mods", description = "Update mod versions for the specified profile")
    public void updateMods(
            @Parameters(arity = "0..1", description = "Name of the profile", paramLabel = "PROFILE") Optional<String> name,
            @Option(names = "-all", description = "Update mods for all profiles, ignoring the selected profile") boolean all
    ) {
        if (all) {
            for (Profile profile : Profile.listProfiles()) {
                updateMods(Optional.of(profile.getName()), false);
            }
            return;
        }

        if (!checkOrSelectProfile(name)) {
            return;
        }

        if (profile.updateMods()) {
            System.out.println("Mods updated successfully for profile: " + profile.getName());
        } else {
            System.out.println("Failed to update mods for profile: " + profile.getName());
        }
    }

    @Command(name = "build-manifest", description = "Build the manifest for the specified profile")
    public void buildManifest(
            @Parameters(arity = "0..1", description = "Name of the profile", paramLabel = "PROFILE") Optional<String> name,
            @Option(names = "-force", description = "Force regeneration of the manifest, even if it already exists") boolean force,
            @Option(names = "-all", description = "Build manifest for all profiles, ignoring the selected profile") boolean all
    ) {
        if (all) {
            for (Profile profile : Profile.listProfiles()) {
                buildManifest(Optional.of(profile.getName()), force, false);
            }
            return;
        }

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
            @Parameters(arity = "0..1", description = "Name of the profile", paramLabel = "PROFILE") Optional<String> name,
            @Option(names = "-force-download", description = "Force redownload of mods, even if they are already downloaded") boolean forceDownload,
            @Option(names = "-all", description = "Download mods for all profiles, ignoring the selected profile") boolean all
    ) {
        if (all) {
            for (Profile profile : Profile.listProfiles()) {
                buildDownloadMods(Optional.of(profile.getName()), forceDownload, false);
            }
            return;
        }

        if (!checkOrSelectProfile(name)) {
            return;
        }

        if (profile.buildDownload(forceDownload)) {
            System.out.println("Mods downloaded successfully for profile: " + profile.getName());
        } else {
            System.out.println("Failed to download mods for profile: " + profile.getName());
        }
    }

    @Command(name = "build-dump", description = "Dump factorio data for the specified profile")
    public void buildDumpDataRaw(
            @Parameters(arity = "0..1", description = "Name of the profile", paramLabel = "PROFILE") Optional<String> name,
            @Option(names = "-force", description = "Force regeneration of data.raw, even if it already exists") boolean force,
            @Option(names = "-all", description = "Dump data for all profiles, ignoring the selected profile") boolean all
    ) {
        if (all) {
            for (Profile profile : Profile.listProfiles()) {
                buildDumpDataRaw(Optional.of(profile.getName()), force, false);
            }
            return;
        }

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
            @Parameters(arity = "0..1", description = "Name of the profile", paramLabel = "PROFILE") Optional<String> name,
            @Option(names = "-force", description = "Force regeneration of data, even if it already exists") boolean force,
            @Option(names = "-all", description = "Generate data for all profiles, ignoring the selected profile") boolean all
    ) {
        if (all) {
            List<Profile> profiles = new ArrayList<>(Profile.listProfiles());

            //Vanilla profile must be built first
            Profile vanillaProfile = Profile.vanilla();
            if (!vanillaProfile.isValid()) {
                System.out.println("No vanilla profile found, it must be created first using command 'profile default-vanilla'");
                return;
            }
            profiles.remove(vanillaProfile);
            profiles.add(0, vanillaProfile);

            for (Profile profile : profiles) {
                buildGenerateData(Optional.of(profile.getName()), force, false);
            }
            return;
        }

        if (!checkOrSelectProfile(name)) {
            return;
        }

        if (profile.buildData(force)) {
            System.out.println("Data generated successfully for profile: " + profile.getName());
        } else {
            System.out.println("Failed to generate data for profile: " + profile.getName());
        }
    }

    @Command(name = "build", description = "Build all steps for the specified profile")
    public void buildAllSteps(
            @Parameters(arity = "0..1", description = "Name of the profile", paramLabel = "PROFILE") Optional<String> name,
            @Option(names = "-all", description = "Build all steps for all profiles") boolean all,
            @Option(names = "-force", description = "Force regeneration of all steps, even if they already exist") boolean force,
            @Option(names = "-force-download", description = "Force redownload of mods, even if they are already downloaded") boolean forceDownload,
            @Option(names = "-force-dump", description = "Force regeneration of factorio dump") boolean forceDump,
            @Option(names = "-force-data", description = "Force regeneration of data") boolean forceData
    ) {
        if (!all && !checkOrSelectProfile(name)) {
            return;
        }

        Profile profileVanilla = Profile.vanilla();

        if (!profileVanilla.isValid()) {
            System.out.println("No vanilla profile found, it must be created first using command 'profile default-vanilla'");
            return;
        }

        List<Profile> profiles;
        if (all) {
            profiles = new ArrayList<>(Profile.listProfiles());
        
            //Put vanilla profile first in the list
            profiles.remove(profileVanilla);
            profiles.add(0, profileVanilla);

        } else {
            profiles = ImmutableList.of(profile);
        }

        if (forceData) {
            for (Profile profile : profiles) {
                profile.cleanData();
            }
        }

        if (forceDump) {
            for (Profile profile : profiles) {
                profile.cleanDump();
            }
        }
        
        for (Profile profile : profiles) {
            if (force || profile.getStatus() == ProfileStatus.BUILD_MANIFEST) {
                buildManifest(Optional.of(profile.getName()), force, false);
            }
        }

        for (Profile profile : profiles) {
            if (forceDownload || profile.getStatus() == ProfileStatus.BUILD_DOWNLOAD) {
                buildDownloadMods(Optional.of(profile.getName()), forceDownload, false);
            }
        }

        for (Profile profile : profiles) {
            if (force || profile.getStatus() == ProfileStatus.BUILD_DUMP) {
                buildDumpDataRaw(Optional.of(profile.getName()), force, false);
            }
        }

        for (Profile profile : profiles) {
            if (force || profile.getStatus() == ProfileStatus.BUILD_DATA) {
                buildGenerateData(Optional.of(profile.getName()), force, false);
            }
        }

        if (Profile.listProfiles().stream().allMatch(Profile::isReady)) {
            System.out.println();
            System.out.println("All profiles are ready! You can now run the bot using command 'bot run'");
        
        } else {
            System.out.println();
            System.out.println("Not all profiles are ready!");
            listProfiles(null, false);
        }
    }

    @Command(name = "clean-manifest", description = "Clean the manifest for the specified profile")
    public void cleanManifest(
            @Parameters(arity = "0..1", description = "Name of the profile", paramLabel = "PROFILE") Optional<String> name,
            @Option(names = "-all", description = "Clean manifest for all profiles, ignoring the selected profile") boolean all
    ) {
        if (all) {
            for (Profile profile : Profile.listProfiles()) {
                cleanManifest(Optional.of(profile.getName()), false);
            }
            return;
        }

        if (!checkOrSelectProfile(name)) {
            return;
        }

        if (profile.cleanManifest()) {
            System.out.println("Manifest cleaned successfully for profile: " + profile.getName());
        }
    }

    @Command(name = "clean-download", description = "Clean all downloaded mods for the specified profile")
    public void cleanDownload(
            @Parameters(arity = "0..1", description = "Name of the profile", paramLabel = "PROFILE") Optional<String> name,
            @Option(names = "-all", description = "Clean downloaded mods for all profiles, ignoring the selected profile") boolean all
    ) {
        if (all) {
            for (Profile profile : Profile.listProfiles()) {
                cleanDownload(Optional.of(profile.getName()), false);
            }
            return;
        }

        if (!checkOrSelectProfile(name)) {
            return;
        }

        if (profile.cleanAllDownloads()) {
            System.out.println("Downloaded mods cleaned successfully for profile: " + profile.getName());
        }
    }

    @Command(name = "clean-download-invalid", description = "Clean invalid downloaded mods for the specified profile")
    public void cleanDownloadInvalid(
            @Parameters(arity = "0..1", description = "Name of the profile", paramLabel = "PROFILE") Optional<String> name,
            @Option(names = "-all", description = "Clean invalid downloaded mods for all profiles, ignoring the selected profile") boolean all
    ) {
        if (all) {
            for (Profile profile : Profile.listProfiles()) {
                cleanDownloadInvalid(Optional.of(profile.getName()), false);
            }
            return;
        }

        if (!checkOrSelectProfile(name)) {
            return;
        }

        if (profile.cleanInvalidDownloads()) {
            System.out.println("Invalid downloaded mods cleaned successfully for profile: " + profile.getName());
        }
    }

    @Command(name = "clean-dump", description = "Clean dumped factorio data for the specified profile")
    public void cleanDumpDataRaw(
            @Parameters(arity = "0..1", description = "Name of the profile", paramLabel = "PROFILE") Optional<String> name,
            @Option(names = "-all", description = "Clean dumped data for all profiles, ignoring the selected profile") boolean all
    ) {
        if (all) {
            for (Profile profile : Profile.listProfiles()) {
                cleanDumpDataRaw(Optional.of(profile.getName()), false);
            }
            return;
        }

        if (!checkOrSelectProfile(name)) {
            return;
        }

        if (profile.cleanDump()) {
            System.out.println("Factorio data dump cleaned successfully for profile: " + profile.getName());
        }
    }

    @Command(name = "clean-data", description = "Clean generated data for the specified profile")
    public void cleanGenerateData(
            @Parameters(arity = "0..1", description = "Name of the profile", paramLabel = "PROFILE") Optional<String> name,
            @Option(names = "-all", description = "Clean generated data for all profiles, ignoring the selected profile") boolean all
    ) {
        if (all) {
            for (Profile profile : Profile.listProfiles()) {
                cleanGenerateData(Optional.of(profile.getName()), false);
            }
            return;
        }

        if (!checkOrSelectProfile(name)) {
            return;
        }

        if (profile.cleanData()) {
            System.out.println("Generated data cleaned successfully for profile: " + profile.getName());
        }
    }

    @Command(name = "clean", description = "Clean all build and data for the specified profile")
    public void cleanAllSteps(
            @Parameters(arity = "0..1", description = "Name of the profile", paramLabel = "PROFILE") Optional<String> name,
            @Option(names = "-all", description = "Clean all data for all profiles, ignoring the selected profile") boolean all,
            @Option(names = "-delete-build", description = "Delete build folder (including downloaded mods) when cleaning all data") boolean deleteBuild
    ) {
        if (!all && !checkOrSelectProfile(name)) {
            return;
        }

        cleanGenerateData(name, all);
        cleanDumpDataRaw(name, all);
        if (deleteBuild) {
            cleanDownload(name, all);
        }
        cleanManifest(name, all);

        if (deleteBuild) {
            if (all) {
                for (Profile profile : Profile.listProfiles()) {
                    if (!profile.deleteBuild()) {
                        System.out.println("Failed to delete build folder for profile: " + profile.getName());
                    }
                }

            } else {
                if (!profile.deleteBuild()) {
                    System.out.println("Failed to delete build folder for profile: " + profile.getName());
                }
            }
        }
        
    }

    @Command(name = "clean-build", description = "Clean the build folder for the specified profile")
    public void cleanBuildFolder(
            @Parameters(arity = "0..1", description = "Name of the profile", paramLabel = "PROFILE") Optional<String> name,
            @Option(names = "-all", description = "Clean build folder for all profiles, ignoring the selected profile") boolean all
    ) {
        if (all) {
            for (Profile profile : Profile.listProfiles()) {
                cleanBuildFolder(Optional.of(profile.getName()), false);
            }
            return;
        }

        if (!checkOrSelectProfile(name)) {
            return;
        }

        if (profile.deleteBuild()) {
            System.out.println("Build folder deleted successfully for profile: " + profile.getName());
        }
    }

    @Command(name = "test", description = "Render test blueprints for the specified profile")
    public void testRender(
            @Parameters(arity = "0..1", description = "Name of the profile", paramLabel = "PROFILE") Optional<String> name,
            @Option(names = "-all", description = "Render test blueprints for all profiles, ignoring the selected profile") boolean all,
            @Option(names = "-all-enabled", description = "Render test blueprints for all enabled profiles, ignoring the selected profile") boolean allEnabled,
            @Option(names = "-all-ready", description = "Render test blueprints for all ready profiles, ignoring the selected profile") boolean allReady
    ) {
        if (!checkOrSelectProfile(name)) {
            return;
        }

        List<Profile> profiles;
        if (all || allEnabled || allReady) {
            Stream<Profile> stream = Profile.listProfiles().stream();
            if (allEnabled) {
                stream = stream.filter(Profile::isEnabled);
            } else if (allReady) {
                stream = stream.filter(Profile::isReady);
            }
            profiles = new ArrayList<>(stream.collect(Collectors.toList()));
        } else {
            profiles = ImmutableList.of(profile);
        }

        boolean checkFail = false;
        for (Profile profile : profiles) {
            if (!profile.isEnabled()) {
                System.out.println("Profile is not enabled: " + profile.getName());
                checkFail = true;
            } else if (!profile.isReady()) {
                System.out.println("Profile is not ready: " + profile.getName());
                checkFail = true;
            }
        }
        if (checkFail) {
            return;
        }

        for (Profile profile : profiles) {
            if (!profile.deleteTests()) {
                System.out.println("Failed to delete old test files for profile: " + profile.getName());
                return;
            }
        }

        for (Profile profile : profiles) {
            if (!profile.renderTests()) {
                System.out.println("Failed to render test blueprints for profile: " + profile.getName());
                return;
            }
        }
    }
}