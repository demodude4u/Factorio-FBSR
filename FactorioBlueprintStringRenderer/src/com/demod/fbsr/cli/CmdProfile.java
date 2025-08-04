package com.demod.fbsr.cli;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONException;
import org.json.JSONObject;

import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.prototype.DataPrototype;
import com.demod.fbsr.Dir16;
import com.demod.fbsr.FactorioManager;
import com.demod.fbsr.Profile;
import com.demod.fbsr.Profile.ProfileStatus;
import com.demod.fbsr.Profile.ProfileWarning;
import com.demod.fbsr.RenderRequest.Debug;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = " ", description = "Configure profiles for mods")
public class CmdProfile {

    private static class ProfileOrAll {
        @Parameters(arity = "0..1", description = "Name of the profile", paramLabel = "<PROFILE>") Optional<String> name;
        @Option(names = "-all", description = "Apply to all profiles") boolean all;
        
        public static ProfileOrAll of(String name) {
            ProfileOrAll profileOrAll = new ProfileOrAll();
            profileOrAll.name = Optional.of(name);
            profileOrAll.all = false;
            return profileOrAll;
        }

        public static ProfileOrAll all() {
            ProfileOrAll profileOrAll = new ProfileOrAll();
            profileOrAll.name = Optional.empty();
            profileOrAll.all = true;
            return profileOrAll;
        }
    }

    @Command(name = "profile-new", description = "Create a new profile")
    public static void createNew(
            @Parameters(arity = "1", description = "Name of the profile to select", paramLabel = "<PROFILE>") String name,
            @Parameters(description = "List of mods to include in the profile", paramLabel = "<MODS>") String[] mods
    ) {
        Profile profile = Profile.byName(name);
        if (profile.generateProfile(mods)) {
            System.out.println("Profile created successfully:" + profile.getName() + " (" + profile.getFolderProfile().getAbsolutePath() + ")");
            System.out.println("To generate a new rendering table, run the command 'profile-default-renderings " + profile.getName() + "'");

            if (!profile.updateMods()) {
                System.out.println("Failed to update mods for profile: " + profile.getName());
            }
        } else {
            System.out.println("Failed to create profile!");
        }
    }

    @Command(name = "profile-default-vanilla", description = "Generate default vanilla profile")
    public static void generateDefaultVanillaProfile(
            @Option(names = "-force", description = "Force regeneration of the default vanilla profile, even if it already exists") boolean force
    ) {
        if (Profile.generateDefaultVanillaProfile(force)) {
            System.out.println("Default vanilla profile created successfully. You can build the profile by running the command 'build vanilla'");
        } else {
            System.out.println("Failed to create default vanilla profile.");
        }
    }

    @Command(name = "profile-status", description = "Get the status of a profile")
    public static void getProfileStatus(
            @Parameters(arity = "1", description = "Name of the profile", paramLabel = "<PROFILE>") String name,
            @Option(names = "-detailed", description = "Include detailed information about the profile") boolean detailed
    ) {
        Profile profile = Profile.byName(name);
        if (!profile.isValid()) {
            System.out.println("Profile not found or invalid: " + name);
            return;
        }

        List<ProfileWarning> warnings = profile.getWarnings();
        System.out.println();
        System.out.println(profile.getStateCode()+ " " + profile.getName() 
                + " (" + profile.getStatus() + ")"
                + (warnings.isEmpty() ? "" : warnings.stream().map(ProfileWarning::name).collect(Collectors.joining(",", " <<", ">>"))));

        if (detailed) {
            System.out.println();
            System.out.println("Folder: " + profile.getFolderProfile().getAbsolutePath());
            System.out.println("Build Folder: " + profile.getFolderBuild().getAbsolutePath());
            System.out.println("Assets File: " + profile.getFileAssets().getAbsolutePath());
            System.out.println();
            System.out.println("Manifest:        " + (profile.hasManifest() ? "Yes" : "No"));
            System.out.println("Mods Downloaded: " + (profile.hasDownloaded() ? "Yes" : "No"));
            System.out.println("Factorio Dump:   " + (profile.hasDump() ? "Yes" : "No"));
            System.out.println("Assets Generated:  " + (profile.hasAssets() ? "Yes" : "No"));
        }

        System.out.println();
        switch (profile.getStatus()) {
            case BUILD_MANIFEST:
                System.out.println("Profile is in BUILD_MANIFEST status. Next step is to run command 'build-manifest " + name + "' or 'build " + name + "'");
                break;
            case BUILD_DOWNLOAD:
                System.out.println("Profile is in BUILD_DOWNLOAD status. Next step is to run command 'build-download " + name + "' or 'build " + name + "'");
                break;
            case BUILD_DUMP:
                System.out.println("Profile is in BUILD_DUMP status. Next step is to run command 'build-dump " + name + "' or 'build " + name + "'");
                break;
            case BUILD_ASSETS:
                System.out.println("Profile is in BUILD_ASSETS status. Next step is to run command 'build-assets " + name + "' or 'build " + name + "'");
                break;
            case READY:
                System.out.println("Profile is in READY status. To run the bot, use command 'bot-run'");
                break;
            case DISABLED:
                System.out.println("Profile is in DISABLED status. It will be ignored when running the bot. To enable this profile, use command 'profile-enable " + name + "'");
                break;
            case INVALID:
                System.out.println("Profile is in INVALID status. It cannot be used until fixed. The profile needs to have a profile.json configured. You can generate a new profile using the command 'profile-new <name> <mod1> <mod2> <mod3> ...'");
                break;
            case NEED_FACTORIO_INSTALL:
                System.out.println("Profile is in NEED_FACTORIO_INSTALL status. Run command `help cfg-factorio` to see details on how to set up Factorio.");
                break;
            case NEED_MOD_PORTAL_CREDENTIALS:
                System.out.println("Profile is in NEED_MOD_PORTAL_CREDENTIALS status. Run command `help cfg-factorio` to see details on how to set up Mod Portal credentials.");
                break;
        }
    }

    @Command(name = "profile-list", description = "List all profiles or mods")
    public static void listProfiles(
            @Option(names = "-filter", description = "Filter profiles by name (partial match)", paramLabel = "<PROFILE>") String filter,
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
            List<ProfileWarning> warnings = profile.getWarnings();
            System.out.println(profile.getStateCode()+ " " + profile.getName() 
                    + " (" + profile.getStatus() + ")"
                    + (warnings.isEmpty() ? "" : warnings.stream().map(ProfileWarning::name).collect(Collectors.joining(",", " <<", ">>"))));
            if (detailed) {
                List<String> mods = profile.listMods();
                for (String mod : mods) {
                    System.out.println(" > " + mod);
                }
            }
        }

        if (!profiles.stream().anyMatch(Profile::isVanilla)) {
            System.out.println();
            System.out.println("WARNING: No vanilla profile found. You need to create one using command 'profile-default-vanilla'.");
        }
    }

    @Command(name = "profile-disable", description = "Disable a profile")
    public static void disableProfile(
            @ArgGroup(exclusive = true, multiplicity = "1") ProfileOrAll profileOrAll
    ) {
        if (profileOrAll.all) {
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

        Profile profile = Profile.byName(profileOrAll.name.get());
        if (!profile.isValid()) {
            System.out.println("Profile not found or invalid: " + profileOrAll.name.get());
            return;
        }

        if (profile.setEnabled(false)) {
            System.out.println("Profile disabled successfully: " + profile.getName());
        } else {
            System.out.println("Failed to disable profile: " + profile.getName());
        }
    }

    @Command(name = "profile-enable", description = "Enable a profile")
    public static void enableProfile(
            @ArgGroup(exclusive = true, multiplicity = "1") ProfileOrAll profileOrAll
    ) {
        if (profileOrAll.all) {
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

        Profile profile = Profile.byName(profileOrAll.name.get());
        if (!profile.isValid()) {
            System.out.println("Profile not found or invalid: " + profileOrAll.name.get());
            return;
        }

        if (profile.setEnabled(true)) {
            System.out.println("Profile enabled successfully: " + profile.getName());

            List<ProfileWarning> warnings = profile.getWarnings();
            System.out.println(profile.getStateCode()+ " " + profile.getName() 
                + " (" + profile.getStatus() + ")"
                + (warnings.isEmpty() ? "" : warnings.stream().map(ProfileWarning::name).collect(Collectors.joining(",", " <<", ">>"))));
        } else {
            System.out.println("Failed to enable profile: " + profile.getName());
        }
    }

    @Command(name = "profile-delete", description = "Delete a profile")
    public static void deleteProfile(
            @Parameters(arity = "1", description = "Name of the profile", paramLabel = "<PROFILE>") String name,
            @Option(names = "-confirm", description = "Skip confirmation prompt") boolean confirm
    ) {
        Profile profile = Profile.byName(name);
        if (!profile.getFolderProfile().exists()) {
            System.out.println("Profile not found: " + name);
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
        } else {
            System.out.println("Failed to delete profile: " + profile.getName());
        }
    }

    @Command(name = "profile-factorio", description = "Run Factorio with the specified profile")
    public static void runFactorio(
            @Parameters(arity = "1", description = "Name of the profile", paramLabel = "<PROFILE>") String name
    ) {
        Profile profile = Profile.byName(name);
        if (!profile.isValid()) {
            System.out.println("Profile not found or invalid: " + name);
            return;
        }

        if (!profile.runFactorio()) {
            System.out.println("Failed to run Factorio with profile: " + profile.getName());
        }
    }

    private static class ExploreAlternatives {
        @Option(names = "-build", description = "Open the build folder instead of the profile folder") boolean build;
        @Option(names = "-assets", description = "Open the assets folder instead of the profile folder") boolean assets;
    }

    @Command(name = "profile-explore", description = "Open file manager for the specified profile")
    public static void explore(
            @Parameters(arity = "1", description = "Name of the profile", paramLabel = "<PROFILE>") String name,
            @ArgGroup(exclusive = true) ExploreAlternatives exploreAlternatives
    ) {
        Profile profile = Profile.byName(name);
        if (!exploreAlternatives.build && !profile.getFolderProfile().exists()) {
            System.out.println("Profile not found: " + name);
            return;
        }
        if (exploreAlternatives.build && !profile.getFolderBuild().exists()) {
            System.out.println("Profile build not found: " + name);
            return;
        }
        if (exploreAlternatives.assets && !profile.getFileAssets().exists()) {
            System.out.println("Profile assets not found: " + name);
            return;
        }

        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(
                        exploreAlternatives.build ? profile.getFolderBuild() : 
                        exploreAlternatives.assets ? profile.getFileAssets() : 
                        profile.getFolderProfile());
            } catch (IOException e) {
                System.out.println("Failed to open profile in file manager: " + e.getMessage());
            }
        } else {
            System.out.println("Desktop is not supported. Cannot open profile in file manager.");
        }
    }

    @Command(name = "profile-edit", description = "Open the profile configuration file in the default editor")
    public static void editProfile(
            @Parameters(arity = "1", description = "Name of the profile", paramLabel = "<PROFILE>") String name
    ) {
        Profile profile = Profile.byName(name);
        if (!profile.isValid()) {
            System.out.println("Profile not found or invalid: " + name);
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

    @Command(name = "profile-update-mods", description = "Update mod versions")
    public static void updateMods(
            @ArgGroup(exclusive = true, multiplicity = "1") ProfileOrAll profileOrAll
    ) {
        if (profileOrAll.all) {
            for (Profile profile : Profile.listProfiles()) {
                updateMods(ProfileOrAll.of(profile.getName()));
            }
            return;
        }

        Profile profile = Profile.byName(profileOrAll.name.get());
        if (!profile.isValid()) {
            System.out.println("Profile not found or invalid: " + profileOrAll.name.get());
            return;
        }

        if (profile.updateMods()) {
            System.out.println("Mods updated successfully for profile: " + profile.getName());
        } else {
            System.out.println("Failed to update mods for profile: " + profile.getName());
        }
    }

    @Command(name = "build-manifest", description = "Build the manifest")
    public static void buildManifest(
            @ArgGroup(exclusive = true, multiplicity = "1") ProfileOrAll profileOrAll,
            @Option(names = "-force", description = "Force regeneration of the manifest, even if it already exists") boolean force
    ) {
        if (profileOrAll.all) {
            for (Profile profile : Profile.listProfiles()) {
                buildManifest(ProfileOrAll.of(profile.getName()), force);
            }
            return;
        }

        Profile profile = Profile.byName(profileOrAll.name.get());
        if (!profile.isValid()) {
            System.out.println("Profile not found or invalid: " + profileOrAll.name.get());
            return;
        }

        if (profile.buildManifest(force)) {
            System.out.println("Manifest built successfully for profile: " + profile.getName());
        } else {
            System.out.println("Failed to build manifest for profile: " + profile.getName());
        }
    }

    @Command(name = "build-download", description = "Download mods")
    public static void buildDownloadMods(
            @ArgGroup(exclusive = true, multiplicity = "1") ProfileOrAll profileOrAll,
            @Option(names = "-force-download", description = "Force redownload of mods, even if they are already downloaded") boolean forceDownload
    ) {
        if (profileOrAll.all) {
            for (Profile profile : Profile.listProfiles()) {
                buildDownloadMods(ProfileOrAll.of(profile.getName()), forceDownload);
            }
            return;
        }

        Profile profile = Profile.byName(profileOrAll.name.get());
        if (!profile.isValid()) {
            System.out.println("Profile not found or invalid: " + profileOrAll.name.get());
            return;
        }

        if (profile.buildDownload(forceDownload)) {
            System.out.println("Mods downloaded successfully for profile: " + profile.getName());
        } else {
            System.out.println("Failed to download mods for profile: " + profile.getName());
        }
    }

    @Command(name = "build-dump", description = "Dump factorio data")
    public static void buildDumpDataRaw(
            @ArgGroup(exclusive = true, multiplicity = "1") ProfileOrAll profileOrAll,
            @Option(names = "-force", description = "Force regeneration of the manifest, even if it already exists") boolean force
    ) {
        if (profileOrAll.all) {
            for (Profile profile : Profile.listProfiles()) {
                buildDumpDataRaw(ProfileOrAll.of(profile.getName()), force);
            }
            return;
        }

        Profile profile = Profile.byName(profileOrAll.name.get());
        if (!profile.isValid()) {
            System.out.println("Profile not found or invalid: " + profileOrAll.name.get());
            return;
        }

        if (profile.buildDump(force)) {
            System.out.println("Factorio data dumped successfully for profile: " + profile.getName());
        } else {
            System.out.println("Failed to dump factorio data for profile: " + profile.getName());
        }
    }

    @Command(name = "build-assets", description = "Generate assets")
    public static void buildGenerateAssets(
            @ArgGroup(exclusive = true, multiplicity = "1") ProfileOrAll profileOrAll,
            @Option(names = "-force", description = "Force regeneration of the assets, even if they already exist") boolean force
    ) {
        if (profileOrAll.all) {
            List<Profile> profiles = new ArrayList<>(Profile.listProfiles());

            //Vanilla profile must be built first
            Profile vanillaProfile = Profile.vanilla();
            if (!vanillaProfile.isValid()) {
                System.out.println("No vanilla profile found, it must be created first using command 'profile-default-vanilla'");
                return;
            }
            profiles.remove(vanillaProfile);
            profiles.add(0, vanillaProfile);

            for (Profile profile : profiles) {
                buildGenerateAssets(ProfileOrAll.of(profile.getName()), force);
            }
            return;
        }

        Profile profile = Profile.byName(profileOrAll.name.get());
        if (!profile.isValid()) {
            System.out.println("Profile not found or invalid: " + profileOrAll.name.get());
            return;
        }

        if (profile.buildAssets(force)) {
            System.out.println("Assets generated successfully for profile: " + profile.getName());
        } else {
            System.out.println("Failed to generate assets for profile: " + profile.getName());
        }
    }

    @Command(name = "build", description = "Build all steps")
    public static void buildAllSteps(
            @ArgGroup(exclusive = true, multiplicity = "1") ProfileOrAll profileOrAll,
            @Option(names = "-force", description = "Force regeneration of all steps, even if they already exist") boolean force,
            @Option(names = "-force-download", description = "Force redownload of mods, even if they are already downloaded") boolean forceDownload,
            @Option(names = "-force-dump", description = "Force regeneration of factorio dump") boolean forceDump,
            @Option(names = "-force-assets", description = "Force regeneration of assets") boolean forceAssets
    ) {
        Profile profileVanilla = Profile.vanilla();

        if (!profileVanilla.isValid()) {
            System.out.println("No vanilla profile found, it must be created first using command 'profile-default-vanilla'");
            return;
        }

        List<Profile> profiles;
        if (profileOrAll.all) {
            profiles = new ArrayList<>(Profile.listProfiles());
        
            //Put vanilla profile first in the list
            profiles.remove(profileVanilla);
            profiles.add(0, profileVanilla);

        } else {
            Profile profile = Profile.byName(profileOrAll.name.get());
            if (!profile.isValid()) {
                System.out.println("Profile not found or invalid: " + profileOrAll.name.get());
                return;
            }

            profiles = ImmutableList.of(profile);
        }

        if (forceAssets) {
            for (Profile profile : profiles) {
                profile.cleanAssets();
            }
        }

        if (forceDump) {
            for (Profile profile : profiles) {
                profile.cleanDump();
            }
        }
        
        for (Profile profile : profiles) {
            if (force || profile.getStatus() == ProfileStatus.BUILD_MANIFEST) {
                buildManifest(ProfileOrAll.of(profile.getName()), force);
            }
        }

        for (Profile profile : profiles) {
            if (forceDownload || profile.getStatus() == ProfileStatus.BUILD_DOWNLOAD) {
                buildDownloadMods(ProfileOrAll.of(profile.getName()), forceDownload);
            }
        }

        for (Profile profile : profiles) {
            if (force || profile.getStatus() == ProfileStatus.BUILD_DUMP) {
                buildDumpDataRaw(ProfileOrAll.of(profile.getName()), force);
            }
        }

        for (Profile profile : profiles) {
            if (force || profile.getStatus() == ProfileStatus.BUILD_ASSETS) {
                buildGenerateAssets(ProfileOrAll.of(profile.getName()), force);
            }
        }

        if (Profile.listProfiles().stream().allMatch(Profile::isReady)) {
            System.out.println();
            System.out.println("All profiles are ready! You can now run the bot using command 'bot-run'");
        
        } else {
            System.out.println();
            System.out.println("Not all profiles are ready!");
            listProfiles(null, false);
        }
    }

    @Command(name = "clean-manifest", description = "Clean the manifest")
    public static void cleanManifest(
            @ArgGroup(exclusive = true, multiplicity = "1") ProfileOrAll profileOrAll
    ) {
        if (profileOrAll.all) {
            for (Profile profile : Profile.listProfiles()) {
                cleanManifest(ProfileOrAll.of(profile.getName()));
            }
            return;
        }

        Profile profile = Profile.byName(profileOrAll.name.get());
        if (!profile.isValid()) {
            System.out.println("Profile not found or invalid: " + profileOrAll.name.get());
            return;
        }

        if (profile.cleanManifest()) {
            System.out.println("Manifest cleaned successfully for profile: " + profile.getName());
        }
    }

    @Command(name = "clean-download", description = "Clean all downloaded mods")
    public static void cleanDownload(
            @ArgGroup(exclusive = true, multiplicity = "1") ProfileOrAll profileOrAll
    ) {
        if (profileOrAll.all) {
            for (Profile profile : Profile.listProfiles()) {
                cleanDownload(ProfileOrAll.of(profile.getName()));
            }
            return;
        }

        Profile profile = Profile.byName(profileOrAll.name.get());
        if (!profile.isValid()) {
            System.out.println("Profile not found or invalid: " + profileOrAll.name.get());
            return;
        }

        if (profile.cleanAllDownloads()) {
            System.out.println("Downloaded mods cleaned successfully for profile: " + profile.getName());
        }
    }

    @Command(name = "clean-download-invalid", description = "Clean invalid downloaded mods")
    public static void cleanDownloadInvalid(
            @ArgGroup(exclusive = true, multiplicity = "1") ProfileOrAll profileOrAll
    ) {
        if (profileOrAll.all) {
            for (Profile profile : Profile.listProfiles()) {
                cleanDownloadInvalid(ProfileOrAll.of(profile.getName()));
            }
            return;
        }

        Profile profile = Profile.byName(profileOrAll.name.get());
        if (!profile.isValid()) {
            System.out.println("Profile not found or invalid: " + profileOrAll.name.get());
            return;
        }

        if (profile.cleanInvalidDownloads()) {
            System.out.println("Invalid downloaded mods cleaned successfully for profile: " + profile.getName());
        }
    }

    @Command(name = "clean-dump", description = "Clean dumped factorio data")
    public static void cleanDumpDataRaw(
            @ArgGroup(exclusive = true, multiplicity = "1") ProfileOrAll profileOrAll
    ) {
        if (profileOrAll.all) {
            for (Profile profile : Profile.listProfiles()) {
                cleanDumpDataRaw(ProfileOrAll.of(profile.getName()));
            }
            return;
        }

        Profile profile = Profile.byName(profileOrAll.name.get());
        if (!profile.isValid()) {
            System.out.println("Profile not found or invalid: " + profileOrAll.name.get());
            return;
        }

        if (profile.cleanDump()) {
            System.out.println("Factorio data dump cleaned successfully for profile: " + profile.getName());
        }
    }

    @Command(name = "clean-assets", description = "Clean generated assets")
    public static void cleanAssets(
            @ArgGroup(exclusive = true, multiplicity = "1") ProfileOrAll profileOrAll
    ) {
        if (profileOrAll.all) {
            for (Profile profile : Profile.listProfiles()) {
                cleanAssets(ProfileOrAll.of(profile.getName()));
            }
            return;
        }

        Profile profile = Profile.byName(profileOrAll.name.get());
        if (!profile.isValid()) {
            System.out.println("Profile not found or invalid: " + profileOrAll.name.get());
            return;
        }

        if (profile.cleanAssets()) {
            System.out.println("Assets cleaned successfully for profile: " + profile.getName());
        }
    }

    @Command(name = "clean", description = "Clean all build and generated data")
    public static void cleanAllSteps(
            @ArgGroup(exclusive = true, multiplicity = "1") ProfileOrAll profileOrAll,
            @Option(names = "-delete-build", description = "Delete build folder (including downloaded mods) when cleaning all data") boolean deleteBuild
    ) {

        cleanAssets(profileOrAll);
        cleanDumpDataRaw(profileOrAll);
        if (deleteBuild) {
            cleanDownload(profileOrAll);
        }
        cleanManifest(profileOrAll);

        if (deleteBuild) {
            if (profileOrAll.all) {
                for (Profile profile : Profile.listProfiles()) {
                    if (!profile.deleteBuild()) {
                        System.out.println("Failed to delete build folder for profile: " + profile.getName());
                    }
                }

            } else {
                Profile profile = Profile.byName(profileOrAll.name.get());
                if (!profile.isValid()) {
                    System.out.println("Profile not found or invalid: " + profileOrAll.name.get());
                    return;
                }

                if (!profile.deleteBuild()) {
                    System.out.println("Failed to delete build folder for profile: " + profile.getName());
                }
            }
        }
        
    }

    @Command(name = "clean-build", description = "Clean the build folder")
    public static void cleanBuildFolder(
            @ArgGroup(exclusive = true, multiplicity = "1") ProfileOrAll profileOrAll
    ) {
        if (profileOrAll.all) {
            for (Profile profile : Profile.listProfiles()) {
                cleanBuildFolder(ProfileOrAll.of(profile.getName()));
            }
            return;
        }

        Profile profile = Profile.byName(profileOrAll.name.get());
        if (!profile.isValid()) {
            System.out.println("Profile not found or invalid: " + profileOrAll.name.get());
            return;
        }

        if (profile.deleteBuild()) {
            System.out.println("Build folder deleted successfully for profile: " + profile.getName());
        }
    }

    @Command(name = "profile-test", description = "Render test blueprints")
    public static void testRender(
            @ArgGroup(exclusive = true, multiplicity = "1") ProfileOrAll profileOrAll,
            @Option(names = "-ignore-disabled", description = "Ignore testing disabled profile(s)") boolean ignoreDisabled,
            @Option(names = "-ignore-not-ready", description = "Ignore testing not ready profile(s)") boolean ignoreNotReady
    ) {
        List<Profile> profiles = new ArrayList<>();
        if (profileOrAll.all) {
            profiles.addAll(Profile.listProfiles());
        
        } else {
            Profile profile = Profile.byName(profileOrAll.name.get());
            if (!profile.isValid()) {
                System.out.println("Profile not found or invalid: " + profileOrAll.name.get());
                return;
            }

            profiles.add(profile);
        }

        if (ignoreDisabled) {
            profiles.removeIf(profile -> !profile.isEnabled());
        }
        if (ignoreNotReady) {
            profiles.removeIf(profile -> !profile.isReady());
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

        boolean openFolder = profiles.size() == 1;
        List<String> failedProfiles = new ArrayList<>();
        for (Profile profile : profiles) {
            if (!profile.renderTests(openFolder)) {
                System.out.println("Failed to render test blueprints for profile: " + profile.getName());
                failedProfiles.add(profile.getName());
            }
        }
        System.out.println();
        if (!failedProfiles.isEmpty()) {
            System.out.println("Some profiles failed to render test blueprints. Check the logs for details.");
            failedProfiles.forEach(profileName -> System.out.println(" - " + profileName));
        } else {
            System.out.println("Test blueprints rendered successfully.");
        }
    }

    @Command(name = "profile-test-entity", description = "Render test image of an entity")
    public static void testEntityRender(
            @Parameters(arity = "1", description = "Name of the profile", paramLabel = "PROFILE") String name,
            @Parameters(arity = "1", description = "Name of the entity", paramLabel = "ENTITY") String entity,
            @Option(names = {"-d", "-dir",  "-direction"}, description = "Direction of the entity (N, NE, NNE, ...)", paramLabel = "<DIR>") Optional<Dir16> direction,
            @Option(names = {"-o", "-orientation"}, description = "Orientation of the entity (0-3, default 0)", paramLabel = "<ORIENTATION>") Optional<Double> orientation,
            @Option(names = {"-c", "-custom"}, description = "JSON object containing entity fields and values", paramLabel = "<JSON>") Optional<String> custom
    ) {
        Profile profile = Profile.byName(name);
        if (!profile.isValid()) {
            System.out.println("Profile not found or invalid: " + name);
            return;
        }

        if (!profile.renderTestEntity(entity, direction, orientation.isPresent() ? OptionalDouble.of(orientation.get()) : OptionalDouble.empty(), custom)) {
            System.out.println("Failed to render test image for entity: " + entity);
        }
    }
}