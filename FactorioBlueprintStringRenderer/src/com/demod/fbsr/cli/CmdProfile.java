package com.demod.fbsr.cli;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.MDC;

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

    private static class ProfileSelect {

        @Parameters(arity = "1..*", description = "Name of the profile", paramLabel = "<PROFILE>") List<String> names;
        @Option(names = "-all", description = "Apply to all profiles") boolean all;

        private boolean requireValid = true;
        private boolean requireEnabled = true;
        
        public static ProfileSelect of(String name) {
            ProfileSelect profileOrAll = new ProfileSelect();
            profileOrAll.names = List.of(name);
            profileOrAll.all = false;
            return profileOrAll;
        }

        public static ProfileSelect all() {
            ProfileSelect profileOrAll = new ProfileSelect();
            profileOrAll.names = List.of();
            profileOrAll.all = true;
            return profileOrAll;
        }

        public void invalidAllowed() {
            requireValid = false;
        }

        public void disabledAllowed() {
            requireEnabled = false;
        }

        public List<Profile> get() {
            List<Profile> profiles = new ArrayList<>();
            if (all) {
                profiles.addAll(Profile.listProfiles());
            } else {
                for (String name : names) {
                    profiles.add(Profile.byName(name));
                }
            }

            return profiles.stream().distinct()
                    .filter(p -> !requireValid || p.isValid())
                    .filter(p -> !requireEnabled || (p.isValid() && p.isEnabled()))
                    .sorted((p1, p2) -> {
                        if (p1.isVanilla()) return -1;
                        if (p2.isVanilla()) return 1;
                        return 0;
                    }).collect(Collectors.toList());
        }

        public static class ActionResult {
            final List<String> messages = new ArrayList<>();
            boolean fail = false;

            public void println(String message) {
                messages.add(message);
            }

            public void fail() {
                fail = true;
            }
        }
        public void forEach(BiConsumer<Profile, ActionResult> action) {
            ActionResult result = new ActionResult();
            List<Profile> profiles = get();
            if (profiles.isEmpty()) {
                System.out.println("No" + (requireValid ? " valid" : "") + (requireEnabled ? " enabled" : "") + " profiles are selected.");
                return;
            }
            for (Profile profile : profiles) {
                MDC.put("profile", profile.getName());
                try {
                    action.accept(profile, result);
                } finally {
                    MDC.remove("profile");
                }
                if (result.fail) {
                    break;
                }
            }
            if (!result.messages.isEmpty()) {
                System.out.println();
                result.messages.forEach(System.out::println);
            }
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
    public static void printProfileStatus(
            @ArgGroup(exclusive = true, multiplicity = "1") ProfileSelect profileSelect,
            @Option(names = "-enabled", description = "Include only enabled profiles") boolean enabledOnly,
            @Option(names = "-detailed", description = "Include detailed information about the profile") boolean detailed
    ) {
        profileSelect.invalidAllowed();
        profileSelect.disabledAllowed();
        profileSelect.forEach((profile, result) -> {
            if (enabledOnly && (!profile.isValid() || !profile.isEnabled())) {
                return;
            }

            if (detailed) {
                System.out.println();
                System.out.println("[[[Profile: " + profile.getName() + "]]]");
                System.out.println("Folder: " + profile.getFolderProfile().getAbsolutePath());
                System.out.println("Build Folder: " + profile.getFolderBuild().getAbsolutePath());
                System.out.println("Assets File: " + profile.getFileAssets().getAbsolutePath());
                System.out.println("Manifest:        " + (profile.hasManifest() ? "Yes" : "No"));
                System.out.println("Mods Downloaded: " + (profile.hasDownloaded() ? "Yes" : "No"));
                System.out.println("Factorio Dump:   " + (profile.hasDump() ? "Yes" : "No"));
                System.out.println("Assets Generated:  " + (profile.hasAssets() ? "Yes" : "No"));
                
                switch (profile.getStatus()) {
                    case BUILD_MANIFEST:
                        System.out.println("Profile is in BUILD_MANIFEST status. Next step is to run command 'build-manifest " + profile.getName() + "' or 'build " + profile.getName() + "'");
                        break;
                    case BUILD_DOWNLOAD:
                        System.out.println("Profile is in BUILD_DOWNLOAD status. Next step is to run command 'build-download " + profile.getName() + "' or 'build " + profile.getName() + "'");
                        break;
                    case BUILD_DUMP:
                        System.out.println("Profile is in BUILD_DUMP status. Next step is to run command 'build-dump " + profile.getName() + "' or 'build " + profile.getName() + "'");
                        break;
                    case BUILD_ASSETS:
                        System.out.println("Profile is in BUILD_ASSETS status. Next step is to run command 'build-assets " + profile.getName() + "' or 'build " + profile.getName() + "'");
                        break;
                    case READY:
                        System.out.println("Profile is in READY status. To run the bot, use command 'bot-run'");
                        break;
                    case DISABLED:
                        System.out.println("Profile is in DISABLED status. It will be ignored when running the bot. To enable this profile, use command 'profile-enable " + profile.getName() + "'");
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

            List<ProfileWarning> warnings = profile.getWarnings();
            result.println(profile.getStateCode()+ " " + profile.getName() 
                    + " (" + profile.getStatus() + ")"
                    + (warnings.isEmpty() ? "" : warnings.stream().map(ProfileWarning::name).collect(Collectors.joining(",", " <<", ">>"))));
        });
    }

    @Command(name = "profile-disable", description = "Disable a profile")
    public static void disableProfile(
            @ArgGroup(exclusive = true, multiplicity = "1") ProfileSelect profileSelect
    ) {
        profileSelect.invalidAllowed();
        profileSelect.disabledAllowed();
        profileSelect.forEach((profile, result) -> {
            if (!profile.isValid()) {
                result.println("Profile not found or invalid: " + profile.getName());
                return;
            }
            
            if (!profile.isEnabled()) {
                result.println("Profile is already disabled: " + profile.getName());
                return;
            }

            if (profile.setEnabled(false)) {
                result.println("Profile disabled successfully: " + profile.getName());

            } else {
                result.println("Failed to disable profile: " + profile.getName());
            }
        });

        printProfileStatus(profileSelect, false, false);
    }

    @Command(name = "profile-enable", description = "Enable a profile")
    public static void enableProfile(
            @ArgGroup(exclusive = true, multiplicity = "1") ProfileSelect profileSelect
    ) {
        profileSelect.invalidAllowed();
        profileSelect.disabledAllowed();
        profileSelect.forEach((profile, result) -> {
            if (!profile.isValid()) {
                result.println("Profile not found or invalid: " + profile.getName());
                return;
            }
            
            if (profile.isEnabled()) {
                result.println("Profile is already enabled: " + profile.getName());
                return;
            }

            if (profile.setEnabled(true)) {
                result.println("Profile enabled successfully: " + profile.getName());

            } else {
                result.println("Failed to enable profile: " + profile.getName());
            }
        });

        printProfileStatus(profileSelect, false, false);
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
            @ArgGroup(exclusive = true, multiplicity = "1") ProfileSelect profileSelect
    ) {
        profileSelect.forEach((profile, result) -> {
            if (profile.updateMods()) {
                result.println("Mods updated successfully for profile: " + profile.getName());
            } else {
                result.println("Failed to update mods for profile: " + profile.getName());
            }
        });

        printProfileStatus(profileSelect, true, false);
    }

    @Command(name = "build-manifest", description = "Build the manifest")
    public static void buildManifest(
            @ArgGroup(exclusive = true, multiplicity = "1") ProfileSelect profileSelect,
            @Option(names = "-force", description = "Force regeneration of the manifest, even if it already exists") boolean force
    ) {
        profileSelect.forEach((profile, result) -> {
            if (profile.buildManifest(force)) {
                result.println("Manifest built successfully for profile: " + profile.getName());
            } else {
                result.println("Failed to build manifest for profile: " + profile.getName());
            }
        });

        printProfileStatus(profileSelect, true, false);
    }

    @Command(name = "build-download", description = "Download mods")
    public static void buildDownloadMods(
            @ArgGroup(exclusive = true, multiplicity = "1") ProfileSelect profileSelect
    ) {
        profileSelect.forEach((profile, result) -> {
            if (profile.buildDownload()) {
                result.println("Mods downloaded successfully for profile: " + profile.getName());
            } else {
                result.println("Failed to download mods for profile: " + profile.getName());
            }
        });

        printProfileStatus(profileSelect, true, false);
    }

    @Command(name = "build-dump", description = "Dump factorio data")
    public static void buildDumpDataRaw(
            @ArgGroup(exclusive = true, multiplicity = "1") ProfileSelect profileSelect,
            @Option(names = "-force", description = "Force regeneration of the manifest, even if it already exists") boolean force
    ) {
        profileSelect.forEach((profile, result) -> {
            if (profile.buildDump(force)) {
                result.println("Factorio data dumped successfully for profile: " + profile.getName());
            } else {
                result.println("Failed to dump factorio data for profile: " + profile.getName());
            }
        });

        printProfileStatus(profileSelect, true, false);
    }

    @Command(name = "build-assets", description = "Generate assets")
    public static void buildGenerateAssets(
            @ArgGroup(exclusive = true, multiplicity = "1") ProfileSelect profileSelect,
            @Option(names = "-force", description = "Force regeneration of the assets, even if they already exist") boolean force
    ) {
        profileSelect.forEach((profile, result) -> {
            if (profile.buildAssets(force)) {
                result.println("Assets generated successfully for profile: " + profile.getName());
            } else {
                result.println("Failed to generate assets for profile: " + profile.getName());
            }
        });

        printProfileStatus(profileSelect, true, false);
    }

    @Command(name = "build", description = "Build all steps")
    public static void buildAllSteps(
            @ArgGroup(exclusive = true, multiplicity = "1") ProfileSelect profileSelect,
            @Option(names = "-force", description = "Force regeneration of all steps, even if they already exist") boolean force,
            @Option(names = "-force-dump", description = "Force regeneration of factorio dump") boolean forceDump,
            @Option(names = "-force-assets", description = "Force regeneration of assets") boolean forceAssets
    ) {
        Profile profileVanilla = Profile.vanilla();

        if (!profileVanilla.isValid()) {
            System.out.println("No vanilla profile found, it must be created first using command 'profile-default-vanilla'");
            return;
        }

        profileSelect.forEach((profile, result) -> {
            if (force || !profile.hasManifest()) {
                if (profile.buildManifest(force)) {
                    result.println("Manifest generated successfully for profile: " + profile.getName());
                } else {
                    result.println("Failed to generate manifest for profile: " + profile.getName());
                }
            }

            if (profile.buildDownload()) {
                result.println("Mods downloaded successfully for profile: " + profile.getName());
            } else {
                result.println("Failed to download mods for profile: " + profile.getName());
            }
            
            if ((force || forceDump) || !profile.hasDump()) {
                if (profile.buildDump((force || forceDump))) {
                    result.println("Factorio data dumped successfully for profile: " + profile.getName());
                } else {
                    result.println("Failed to dump factorio data for profile: " + profile.getName());
                }
            }

            if ((force || forceAssets) || !profile.hasAssets()) {
                if (profile.buildAssets((force || forceAssets))) {
                    result.println("Assets generated successfully for profile: " + profile.getName());
                } else {
                    result.println("Failed to generate assets for profile: " + profile.getName());
                }
            }
        });

        if (profileSelect.all) {
            if (profileSelect.get().stream().allMatch(Profile::isReady)) {
                System.out.println();
                System.out.println("All enabled profiles are ready! You can run the bot using the command 'bot-run'");
            
            } else {
                System.out.println();
                System.out.println("Not all enabled profiles are ready!");
            }
        }

        printProfileStatus(profileSelect, true, false);
    }

    @Command(name = "clean-build", description = "Delete the build files (including downloaded mods)")
    public static void cleanBuildFolder(
            @ArgGroup(exclusive = true, multiplicity = "1") ProfileSelect profileSelect
    ) {
        profileSelect.invalidAllowed();
        profileSelect.disabledAllowed();
        profileSelect.forEach((profile, result) -> {
            if (profile.deleteBuild()) {
                result.println("Build folder deleted successfully for profile: " + profile.getName());
            }
        });

        printProfileStatus(profileSelect, false, false);
    }

    @Command(name = "clean-assets", description = "Delete the generated assets")
    public static void cleanAssets(
            @ArgGroup(exclusive = true, multiplicity = "1") ProfileSelect profileSelect
    ) {
        profileSelect.invalidAllowed();
        profileSelect.disabledAllowed();
        profileSelect.forEach((profile, result) -> {
            if (profile.deleteAssets()) {
                result.println("Assets deleted successfully for profile: " + profile.getName());
            }
        });

        printProfileStatus(profileSelect, false, false);
    }

    @Command(name = "profile-test", description = "Render test blueprints")
    public static void testRender(
            @ArgGroup(exclusive = true, multiplicity = "1") ProfileSelect profileSelect
    ) {
        boolean openFolder = profileSelect.get().size() == 1;
        AtomicBoolean failure = new AtomicBoolean(false);
        profileSelect.forEach((profile, result) -> {
            if (!profile.isReady()) {
                result.println("Profile is not ready: " + profile.getName());
                failure.set(true);
                return;
            }

            if (!profile.deleteTests()) {
                result.println("Failed to delete old test files for profile: " + profile.getName());
                failure.set(true);
                return;
            }

            if (!profile.renderTests(openFolder)) {
                result.println("Failed to render test blueprints for profile: " + profile.getName());
                failure.set(true);
            } else {
                result.println("Test blueprints rendered successfully for profile: " + profile.getName());
            }
        });
        System.out.println();
        if (failure.get()) {
            System.out.println("Some profiles failed to render test blueprints. Check the logs for details.");
        } else {
            System.out.println("Test blueprints rendered successfully!");
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