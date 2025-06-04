package com.demod.fbsr.cli;

import java.awt.Desktop;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.demod.fbsr.FactorioManager;
import com.demod.fbsr.ProfileEditor;

import net.dv8tion.jda.api.entities.User.Profile;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "profile", description = "Configure profiles for mods")
public class CmdProfile {

    private volatile ProfileEditor editor = null;

    @Command(name = "select", description = "Select a profile to work with in interactive mode")
    public void selectProfile(
            @Parameters(arity = "1", description = "Name of the profile to select") String name
    ) {
        ProfileEditor editor = new ProfileEditor();
        if (editor.findProfile(name)) {
            this.editor = editor;
            System.out.println("Selected profile: " + name);
        } else {
            System.out.println("Profile not found: " + name);
        }
    }

    private boolean checkOrSelectProfile(Optional<String> name) {
        if (name.isEmpty()) {
            if (this.editor == null) {
                System.out.println("No profile selected. Use 'profile select <name>' or '-name <name>' to select a profile.");
                return false;
            }
            return true;

        } else {
            ProfileEditor editor = new ProfileEditor();
            if (!editor.findProfile(name.get())) {
                System.out.println("Profile not found: " + name);
                return false;
            }
            this.editor = editor;
            return true;
        }
    }

    @Command(name = "new", description = "Create a new profile")
    public void createNew(
            @Option(names = "-name", required = true, description = "Name of the profile to create") String name,
            @Option(names = "-label", required = true, description = "Mod label for entities and tiles") String group,
            @Option(names = "-force", description = "Force creation of the profile, overwriting existing one") boolean force,
            @Parameters(description = "List of mods to include in the profile") String[] mods
    ) {
        ProfileEditor editor = new ProfileEditor();
        if (editor.generateProfile(name, group, force, mods)) {
            System.out.println("Profile created successfully:" + editor.getFolderMods().getAbsolutePath());
        } else {
            System.out.println("Failed to create profile!");
        }
    }

    @Command(name = "list", description = "List all profiles or mods")
    public void listProfiles(
            @Option(names = "-filter", description = "Filter profiles by name") String filter,
            @Option(names = "-detailed", description = "Include mods in the profile listing") boolean detailed
    ) {
        ProfileEditor editor = new ProfileEditor();
        List<String> profiles = editor.listProfileNames();
        if (profiles.isEmpty()) {
            System.out.println("No profiles found.");
            return;
        }
        System.out.println("Available profiles:");
        for (String profile : profiles) {
            if (filter != null && !profile.contains(filter)) {
                continue;
            }
            System.out.println(" - " + profile);
            if (detailed) {
                editor.findProfile(profile);
                List<String> mods = editor.listMods();
                for (String mod : mods) {
                    System.out.println("      > " + mod);
                }
            }
        }
        
        // TODO show (disabled) and (missing) tags at the end
        // TODO show size of the profile
    }

    @Command(name = "disable", description = "Disable a profile")
    public void disableProfile(
            @Option(names = "-name", description = "Name of the profile") Optional<String> name
    ) {
        if (!checkOrSelectProfile(name)) {
            return;
        }

        // TODO
    }

    @Command(name = "enable", description = "Enable a profile")
    public void enableProfile(
            @Option(names = "-name", description = "Name of the profile") Optional<String> name
    ) {
        if (!checkOrSelectProfile(name)) {
            return;
        }

        // TODO
    }

    @Command(name = "factorio", description = "Run Factorio with the specified profile")
    public void runFactorio(
            @Option(names = "-name", description = "Name of the profile") Optional<String> name
    ) {
        if (!checkOrSelectProfile(name)) {
            return;
        }

        editor.runFactorio();
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
        ProfileEditor editor = new ProfileEditor();
        if (editor.testProfile(mods)) {
            System.out.println("No Errors -- Check dump output to confirm!");
        } else {
            System.out.println("Failed to create test profile!");
        }
    }

}
