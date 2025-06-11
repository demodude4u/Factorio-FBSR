package com.demod.fbsr.cli;

import java.awt.Desktop;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.demod.fbsr.FactorioManager;
import com.demod.fbsr.Profile;

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
            System.out.println("Profile created successfully:" + profile.getName() + " (" + profile.getFolderProfile().getAbsolutePath()) + ")");
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
    public void generateDefaultVanillaProfile() {
        if (Profile.generateDefaultVanillaProfile()) {
            System.out.println("Default vanilla profile created successfully.");
        } else {
            System.out.println("Failed to create default vanilla profile.");
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
            System.out.println(" - " + profile + "(" + profile.getStatus() + ")");
            if (detailed) {
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
            @Option(names = "-name", description = "Name of the profile") Optional<String> name
    ) {
        if (!checkOrSelectProfile(name)) {
            return;
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

        profile.runFactorio();
    }

    @Command(name = "dump", description = "Dump data.raw for the specified profile")
    public void dumpDataRaw(
            @Option(names = "-name", description = "Name of the profile") Optional<String> name,
            @Option(names = "-force", description = "Force regeneration of data.raw, even if it already exists") boolean force
    ) {
        if (!checkOrSelectProfile(name)) {
            return;
        }

        // TODO
    }

    @Command(name = "data", description = "Generate data for the specified profile")
    public void generateData(
            @Option(names = "-name", description = "Name of the profile") Optional<String> name,
            @Option(names = "-force", description = "Force regeneration of data, even if it already exists") boolean force,
            @Option(names = "-dump", description = "Force dump data.raw before generation") boolean forceDump
    ) {
        if (!checkOrSelectProfile(name)) {
            return;
        }

        // TODO
    }

    @Command(name = "explore", description = "Open file manager for the specified profile")
    public void explore(
            @Option(names = "-name", description = "Name of the profile") Optional<String> name
    ) {
        if (!checkOrSelectProfile(name)) {
            return;
        }

        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(editor.getFolderMods());
            } catch (IOException e) {
                System.out.println("Failed to open profile in file manager: " + e.getMessage());
            }
        } else {
            System.out.println("Desktop is not supported. Cannot open profile in file manager.");
        }
    }

    @Command(name = "protos", description = "List prototypes (entities and tiles) for the specified profile")
    public void listPrototypes(
            @Option(names = "-name", description = "Name of the profile") Optional<String> name
    ) {
        if (!checkOrSelectProfile(name)) {
            return;
        }

        // TODO
    }

    @Command(name = "download", description = "Download mods for the specified profile")
    public void downloadMods(
            @Option(names = "-name", description = "Name of the profile") Optional<String> name
    ) {
        if (!checkOrSelectProfile(name)) {
            return;
        }

        try {
            if (!FactorioManager.downloadMods(editor.getFolderMods())) {
                System.out.println("No mods were downloaded for profile: " + name);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Command(name = "validate", description = "Validate the specified profile, checking prototypes and factories")
    public void validateProfile(
            @Option(names = "-name", description = "Name of the profile") Optional<String> name
    ) {
        if (!checkOrSelectProfile(name)) {
            return;
        }

        // TODO
    }

    @Command(name = "test", description = "Runs a dump on a temporary profile")
    public void testDumpDataRaw(
            @Parameters(arity = "1..*", description = "List of mods to include in the profile") String[] mods
    ) {
        Profile editor = new Profile();
        if (editor.testProfile(mods)) {
            System.out.println("No Errors -- Check dump output to confirm!");
        } else {
            System.out.println("Failed to create test profile!");
        }
    }

}
