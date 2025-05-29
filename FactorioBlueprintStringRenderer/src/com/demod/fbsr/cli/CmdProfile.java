package com.demod.fbsr.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "profile", description = "Configure profiles for mods")
public class CmdProfile {

    @Command(name = "new", description = "Create a new profile")
    public void createNew(
            @Option(names = "-name", arity = "0..1", interactive = true) String name,
            @Option(names = "-mods", arity = "0..*", description = "List of mods to include in the profile") String[] mods
    ) {
        //TODO
    }

    @Command(name = "list", description = "List all profiles")
    public void listProfiles() {
        // TODO
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
        // TODO
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
            @Option(names = "-name", required = true, description = "Name of the profile to use") String name,
            @Option(names = "-force", description = "Force redownload of mods, even if they already exist") boolean force
    ) {
        // TODO
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
        // TODO
    }

}
