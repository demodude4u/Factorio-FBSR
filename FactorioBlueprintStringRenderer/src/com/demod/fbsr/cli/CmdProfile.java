package com.demod.fbsr.cli;

import java.io.IOException;
import java.util.List;

import com.demod.fbsr.FactorioManager;
import com.demod.fbsr.ProfileEditor;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "profile", description = "Configure profiles for mods")
public class CmdProfile {

    @Command(name = "new", description = "Create a new profile")
    public void createNew(
            @Option(names = "-name", required = true, description = "Name of the profile to create") String name,
            @Option(names = "-label", required = true, description = "Mod label for entities and tiles") String group,
            @Option(names = "-force", description = "Force creation of the profile, overwriting existing one") boolean force,
            @Parameters(description = "List of mods to include in the profile") String[] mods
    ) {
        ProfileEditor editor = new ProfileEditor();
        if (editor.generateProfile(name, group, force, mods)) {
            System.out.println("Profile created successfully:" + editor.getProfile().getAbsolutePath());
        } else {
            System.out.println("Failed to create profile!");
        }
    }

    @Command(name = "list", description = "List all profiles or mods")
    public void listProfiles(
            @Option(names = "-name", description = "Filter profiles by name") String nameFilter,
            @Option(names = "-mods", description = "Include mods in the profile listing") boolean showMods
    ) {
        ProfileEditor editor = new ProfileEditor();
        List<String> profiles = editor.listProfileNames();
        if (profiles.isEmpty()) {
            System.out.println("No profiles found.");
            return;
        }
        System.out.println("Available profiles:");
        for (String profile : profiles) {
            if (nameFilter != null && !profile.contains(nameFilter)) {
                continue;
            }
            System.out.println(" - " + profile);
            if (showMods) {
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
            @Option(names = "-name", required = true, description = "Name of the profile to disable") String name
    ) {
        // TODO
    }

    @Command(name = "enable", description = "Enable a profile")
    public void enableProfile(
            @Option(names = "-name", required = true, description = "Name of the profile to enable") String name
    ) {
        // TODO
    }

    @Command(name = "factorio", description = "Run Factorio with the specified profile")
    public void runFactorio(
            @Option(names = "-name", required = true, description = "Name of the profile to use") String name
    ) {
        ProfileEditor editor = new ProfileEditor();
        if (!editor.findProfile(name)) {
            System.out.println("Profile not found: " + name);
            return;
        }

        editor.runFactorio();
    }

    @Command(name = "atlases", description = "Generate atlases for the specified profile")
    public void generateAtlases(
            @Option(names = "-name", required = true, description = "Name of the profile to use") String name,
            @Option(names = "-force", description = "Force regeneration of atlases, even if they already exist") boolean force
    ) {
        // TODO
    }

    @Command(name = "dump", description = "Dump data.raw for the specified profile")
    public void dumpDataRaw(
            @Option(names = "-name", required = true, description = "Name of the profile to use") String name,
            @Option(names = "-force", description = "Force regeneration of data.raw, even if it already exists") boolean force
    ) {
        // TODO
    }

    @Command(name = "prototypes", description = "List prototypes (entities and tiles) for the specified profile")
    public void listPrototypes(
            @Option(names = "-name", required = true, description = "Name of the profile to use") String name
    ) {
        // TODO
    }

    @Command(name = "download", description = "Download mods for the specified profile")
    public void downloadMods(
            @Option(names = "-name", required = true, description = "Name of the profile to use") String name
    ) {
        ProfileEditor editor = new ProfileEditor();
        if (!editor.findProfile(name)) {
            System.out.println("Profile not found: " + name);
            return;
        }

        try {
            FactorioManager.downloadMods(editor.getProfile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Command(name = "validate", description = "Validate the specified profile, checking prototypes and factories")
    public void validateProfile(
            @Option(names = "-name", required = true, description = "Name of the profile to validate") String name
    ) {
        // TODO
    }

    @Command(name = "test", description = "Runs a dump on a temporary profile")
    public void testDumpDataRaw(
            @Option(names = "-mods", arity = "0..*", description = "List of mods to include in the profile") String[] mods
    ) {
        ProfileEditor editor = new ProfileEditor();
        if (editor.testProfile(mods)) {
            System.out.println("No Errors -- Check dump output to confirm!");
        } else {
            System.out.println("Failed to create test profile!");
        }
    }

}
