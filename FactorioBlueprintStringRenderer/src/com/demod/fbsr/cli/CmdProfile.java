package com.demod.fbsr.cli;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.MDC;

import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.prototype.DataPrototype;
import com.demod.fbsr.Dir16;
import com.demod.fbsr.EntityRendererFactory;
import com.demod.fbsr.FactorioManager;
import com.demod.fbsr.Profile;
import com.demod.fbsr.RenderingRegistry;
import com.demod.fbsr.TileRendererFactory;
import com.demod.fbsr.Profile.ManifestModInfo;
import com.demod.fbsr.Profile.ProfileStatus;
import com.demod.fbsr.Profile.ProfileWarning;
import com.demod.fbsr.RenderRequest.Debug;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = " ", description = "Configure profiles for mods")
public class CmdProfile {

    private static class ProfileSelect {

        @Parameters(arity = "1..*", description = "Name of the profile", paramLabel = "<PROFILE>") List<String> names;
        @Option(names = {"-a", "-all"}, description = "Apply to all profiles") boolean all;

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
                    .filter(p -> !requireEnabled || p.isEnabled())
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
            @Parameters(arity = "1..*", description = "List of mods to include in the profile", paramLabel = "<MODS>") String[] mods
    ) {
        Profile profile = Profile.byName(name);
        if (profile.generateProfile(mods)) {
            System.out.println("Profile created successfully: " + profile.getName() + " (" + profile.getFolderProfile().getAbsolutePath() + ")");
            
            if (!profile.updateMods()) {
                System.out.println("Failed to update mods for profile: " + profile.getName());
            }

            printProfileStatus(ProfileSelect.of(name), false, true);
        } else {
            System.out.println("Failed to create profile!");
        }
    }

    @Command(name = "profile-default-vanilla", description = "Generate default vanilla profile")
    public static void generateDefaultVanillaProfile(
            @Option(names = {"-f", "-force"}, description = "Force regeneration of the default vanilla profile, even if it already exists") boolean force
    ) {
        Profile profileVanilla = Profile.vanilla();
        if ((profileVanilla.isValid()) && !force) {
            System.out.println("Vanilla profile already exists.");
            return;
        }

        if (Profile.generateDefaultVanillaProfile()) {
            System.out.println("Default vanilla profile created successfully. You can build the profile by running the command 'build vanilla'");
        } else {
            System.out.println("Failed to create default vanilla profile.");
        }
    }

    @Command(name = "profile-status", description = "Get the status of a profile")
    public static void printProfileStatus(
            @ArgGroup(exclusive = true, multiplicity = "1") ProfileSelect profileSelect,
            @Option(names = {"-e", "-enabled"}, description = "Include only enabled profiles") boolean enabledOnly,
            @Option(names = {"-d", "-detailed"}, description = "Include detailed information about the profile") boolean detailed
    ) {
        profileSelect.invalidAllowed();
        profileSelect.disabledAllowed();
        AtomicBoolean first = new AtomicBoolean(true);
        profileSelect.forEach((profile, result) -> {
            if (enabledOnly && !profile.isEnabled()) {
                return;
            }

            if (!profile.isValid()) {
                result.println("Profile not found or invalid: " + profile.getName());
            }

            if (detailed) {
                System.out.println();
                System.out.println("[[[Profile: " + profile.getName() + "]]] -- " + profile.getStatus().name());
                System.out.println("Folder: " + profile.getFolderProfile().getAbsolutePath());
                System.out.println("Build Folder: " + profile.getFolderBuild().getAbsolutePath());
                System.out.println("Assets File: " + profile.getFileAssets().getAbsolutePath());
                System.out.println("Manifest:        " + (profile.hasManifest() ? "Yes" : "No"));
                System.out.println("Mods Downloaded: " + (profile.hasDownloaded() ? "Yes" : "No"));
                System.out.println("Factorio Dump:   " + (profile.hasDump() ? "Yes" : "No"));
                System.out.println("Assets Generated:  " + (profile.hasAssets() ? "Yes" : "No"));

                if (profile.hasManifest() || profile.hasAssets()) {
                    System.out.println("Mods:");
                    profile.listMods().stream()
                            .filter(mod -> !mod.name.equals("base"))
                            .forEach(mod -> System.out.println("  - " + mod.name + (!mod.builtin ? " " + mod.version : "")));
                }
                
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
                    case DISABLED:
                        System.out.println("Profile is in DISABLED status. It will be ignored when running the bot. To enable this profile, use command 'profile-enable " + profile.getName() + "'");
                        break;
                    case INVALID:
                        System.out.println("Profile is in INVALID status. The profile needs to have a profile.json configured. You can generate a new profile using the command 'profile-new " + profile.getName() + " <mod1> <mod2> <mod3> ...'");
                        break;
                    case NEED_FACTORIO:
                        System.out.println("Profile is in NEED_FACTORIO status. Run command `help cfg-factorio` to see details on how to set up Factorio.");
                        break;
                    case NEED_MOD_PORTAL:
                        System.out.println("Profile is in NEED_MOD_PORTAL status. Run command `help cfg-factorio` to see details on how to set up Mod Portal credentials.");
                        break;
                    default:break;
                }
            }

            int modCount = 0;
            int entityCount = 0;
            int tileCount = 0;
            int imageCount = 0;
            if (profile.hasManifest() || profile.hasAssets()) {
                modCount = profile.listMods().size();
            }
            if (profile.hasAssets()) {   
                JSONObject jsonRendering = profile.getAssetsRenderingConfiguration();
                entityCount = Optional.ofNullable(jsonRendering.optJSONObject("entities")).map(JSONObject::length).orElse(0);
                tileCount = Optional.ofNullable(jsonRendering.optJSONObject("tiles")).map(JSONObject::length).orElse(0);
                JSONObject jsonManifest = profile.getAssetsAtlasManifest();
                if (jsonManifest != null && jsonManifest.has("entries")) {
                    JSONArray jsonEntries = jsonManifest.getJSONArray("entries");
                    imageCount = jsonEntries.length();
                }
            }
            List<ProfileWarning> warnings = profile.getWarnings();
            if (first.get()) {
                System.out.println();
                System.out.println(String.format("%-20s | %-4s | %-15s | %-4s | %-6s | %-6s | %-30s", 
                        "Profile", "Code", "Status", "Mods", "Protos", "Images", "Warnings"));
                System.out.println(String.format("%-20s | %-4s | %-15s | %-4s | %-6s | %-6s | %-30s", 
                        "--------------------", "----", "---------------", "----", "------", "------", "------------------------------"));
                first.set(false);
            }
            System.out.println(String.format("%-20s | %-4s | %-15s | %-4s | %-6s | %-6s | %-30s",
                    profile.getName(),
                    profile.getStateCode(),
                    profile.getStatus(),
                    modCount > 0 ? modCount : "",
                    (entityCount + tileCount) > 0 ? (entityCount + tileCount) : "",
                    imageCount > 0 ? imageCount : "",
                    warnings.stream().map(ProfileWarning::name).collect(Collectors.joining(", "))));
        });
    }

    @Command(name = "mod-query", description = "List mods based on filters")
    public static void modQuery(
            @Option(names = {"-m", "-mod"}, description = "Filter by mod", arity = "0..*") List<String> modFilter,
            @Option(names = {"-p", "-profile"}, description = "Filter by profile", arity = "0..*") List<String> profileFilter,
            @Option(names = {"-e", "-entity"}, description = "Filter by entity", arity = "0..*") List<String> entityFilter,
            @Option(names = {"-t", "-tile"}, description = "Filter by tile", arity = "0..*") List<String> tileFilter,
            @Option(names = {"-d", "-detailed"}, description = "Show mod details") boolean detailed
    ) {
        Set<String> mods = new HashSet<>();
        ListMultimap<String, Profile> profileByMod = ArrayListMultimap.create();
        ListMultimap<String, String> entityByMod = ArrayListMultimap.create();
        ListMultimap<String, String> tileByMod = ArrayListMultimap.create();

        ListMultimap<String, ManifestModInfo> modInfoByMod = ArrayListMultimap.create();
        
        for (Profile profile : Profile.listProfiles()) {
            if (!profile.hasAssets()) {
                continue;
            }

            List<ManifestModInfo> profileMods = profile.listMods();
            for (ManifestModInfo modInfo : profileMods) {
                if (modInfo.builtin) {
                    continue;
                }

                String mod = modInfo.name;
                mods.add(mod);
                profileByMod.put(mod, profile);
                modInfoByMod.put(mod, modInfo);
            }

            RenderingRegistry registry = new RenderingRegistry(profile);
            registry.loadConfig(profile.getAssetsRenderingConfiguration());
            for (EntityRendererFactory factory : registry.getEntityFactories()) {
                for (ManifestModInfo mod : factory.getMods()) {
                    mods.add(mod.name);
                    entityByMod.put(mod.name, factory.getName());
                }
            }
            for (TileRendererFactory factory : registry.getTileFactories()) {
                for (ManifestModInfo mod : factory.getMods()) {
                    mods.add(mod.name);
                    tileByMod.put(mod.name, factory.getName());
                }
            }
        }

        ListMultimap<String, String> filteredBy = ArrayListMultimap.create();
        interface MatchAny {
            boolean filter(String mod, String value, List<String> patterns);
        }
        MatchAny matchesAny = (mod, value, patterns) -> {
            boolean match = false;
            for (String pattern : patterns) {
                if (pattern.equals("*") || value.equals(pattern)) {
                    match = true;
                    filteredBy.put(mod, value);
                    continue;
                };
                if (pattern.contains("*")) {
                    String regex = pattern.replace(".", "\\.").replace("*", ".*");
                    if (value.matches(regex)) {
                        match = true;
                        filteredBy.put(mod, value);
                    }
                }
            }
            return match;
        };

        if (modFilter != null && !modFilter.isEmpty()) {
            mods.removeIf(mod -> !matchesAny.filter(mod, mod, modFilter));
        }
        if (profileFilter != null && !profileFilter.isEmpty()) {
            mods.removeIf(mod -> {
                List<Profile> profiles = profileByMod.get(mod);
                return profiles.stream().noneMatch(p -> matchesAny.filter(mod, p.getName(), profileFilter));
            });
        }
        if (entityFilter != null && !entityFilter.isEmpty()) {
            mods.removeIf(mod -> {
                List<String> entities = entityByMod.get(mod);
                return entities.stream().noneMatch(e -> matchesAny.filter(mod, e, entityFilter));
            });
        }
        if (tileFilter != null && !tileFilter.isEmpty()) {
            mods.removeIf(mod -> {
                List<String> tiles = tileByMod.get(mod);
                return tiles.stream().noneMatch(t -> matchesAny.filter(mod, t, tileFilter));
            });
        }

        List<String> modOrder = mods.stream().sorted().collect(Collectors.toList());
        List<String> tableMessage = new ArrayList<>();
        boolean firstTableEntry = true;
        for (String mod : modOrder) {
            List<Profile> profiles = profileByMod.get(mod);
            if (profiles.isEmpty()) {
                continue;
            }

            List<ManifestModInfo> modInfos = modInfoByMod.get(mod);
            List<String> versions = modInfos.stream().map(info -> info.version).distinct().collect(Collectors.toList());

            String titlesStr = modInfos.stream().map(info -> info.title).distinct().sorted().collect(Collectors.joining(", "));
            String versionsStr = versions.stream().sorted().collect(Collectors.joining(", "));
            String profilesStr = profiles.stream().map(Profile::getName).collect(Collectors.joining(", "));
            String entitiesStr = entityByMod.get(mod).stream().sorted().collect(Collectors.joining(", "));
            String tilesStr = tileByMod.get(mod).stream().sorted().collect(Collectors.joining(", "));
            String filteredByStr = filteredBy.get(mod).stream().sorted().collect(Collectors.joining(", "));

            if (detailed) {
                System.out.println();
                System.out.println("<< " + mod + " " + versionsStr + " >> -- " + titlesStr);
                System.out.println("Profile: " + profilesStr);
                System.out.println("Downloads: " + modInfos.stream().mapToLong(info -> info.downloads).max().orElse(0));
                System.out.println("Category: " + modInfos.stream().map(info -> info.category).distinct().collect(Collectors.joining(", ")));
                System.out.println("Tags: " + modInfos.stream().flatMap(info -> info.tags.stream()).distinct().collect(Collectors.joining(", ")));
                System.out.println("Owner: " + modInfos.stream().map(info -> info.owner).distinct().collect(Collectors.joining(", ")));
                System.out.println("Last Updated: " + modInfos.stream().map(info -> info.updated).distinct().sorted(Comparator.reverseOrder()).findFirst().orElse(""));
                System.out.println("Entities: " + (entitiesStr.isBlank() ? "<None>" : entitiesStr));
                System.out.println("Tiles:    " + (tilesStr.isBlank() ? "<None>" : tilesStr));
                if (filteredBy.size() > 0) {
                    System.out.println("Filtered By: " + filteredByStr);
                }
            }

            String modStr = mod.length() > 25 ? mod.substring(0, 25-3) + "..." : mod;
            titlesStr = titlesStr.length() > 30 ? titlesStr.substring(0, 30-3) + "..." : titlesStr;
            profilesStr = profilesStr.length() > 15 ? profilesStr.substring(0, 15-3) + "..." : profilesStr;
            filteredByStr = filteredByStr.length() > 30 ? filteredByStr.substring(0, 30-3) + "..." : filteredByStr;

            if (firstTableEntry) {
                tableMessage.add(String.format("%-25s | %-30s | %-8s | %-15s" + (filteredBy.isEmpty() ? "" : " | %-30s"), 
                        "Mod", "Title", "Version", "Profile", "Filtered By"));
                tableMessage.add(String.format("%-25s | %-30s | %-8s | %-15s" + (filteredBy.isEmpty() ? "" : " | %-30s"),
                        "-------------------------", "------------------------------", "--------", "---------------", "------------------------------"));
                firstTableEntry = false;
            }
            tableMessage.add(String.format("%-25s | %-30s | %-8s | %-15s" + (filteredBy.isEmpty() ? "" : " | %-30s"),
                    modStr,
                    titlesStr,
                    versions.size() > 2 ? "*.*.*" : versionsStr,
                    profilesStr,
                    filteredByStr));
        }

        if (modOrder.isEmpty()) {
            System.out.println();
            System.out.println("No mods matched the query.");
        }

        System.out.println();
        tableMessage.forEach(System.out::println);
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

    private static class DeleteSelection {
        @Option(names = {"-profile"}, description = "Delete the profile folder") boolean profile;
        @Option(names = {"-build"}, description = "Delete the build folder") boolean build;
        @Option(names = {"-assets"}, description = "Delete the assets zip") boolean assets;
    }
    @Command(name = "profile-delete", description = "Delete profile, build, or assets data")
    public static void deleteProfile(
            @ArgGroup(exclusive = true, multiplicity = "1") ProfileSelect profileSelect,
            @ArgGroup(exclusive = false, multiplicity = "1") DeleteSelection deleteSelection,
            @Option(names = {"-c", "-confirm"}, description = "Skip confirmation prompt") boolean confirm
    ) {
        profileSelect.forEach((profile, result) -> {
            if (deleteSelection.profile && profile.getFolderProfile().exists()) {
                result.println("Deleting profile folder: " + profile.getFolderProfile().getName());
            }
            if (deleteSelection.build && profile.getFolderBuild().exists()) {
                result.println("Deleting build folder: " + profile.getFolderBuild().getName());
            }
            if (deleteSelection.assets && profile.getFileAssets().exists()) {
                result.println("Deleting assets file: " + profile.getFileAssets().getName());
            }
        });
        
        List<Profile> profiles = profileSelect.get();
        boolean justOneProfile = profiles.size() == 1;
        if (!confirm) {
            String confirmMatch = justOneProfile ? profiles.get(0).getName() : "confirm";
            System.out.print("Type `" + confirmMatch + "` to confirm deletion: ");
            @SuppressWarnings("resource")
            String input = new java.util.Scanner(System.in).nextLine();
            if (!input.equals(confirmMatch)) {
                System.out.println("Confirmation failed. Deletion aborted.");
                return;
            }
        }

        profileSelect.forEach((profile, result) -> {
            if (deleteSelection.profile && profile.getFolderProfile().exists()) {
                if (profile.deleteProfile()) {
                    result.println("Profile folder deleted successfully: " + profile.getName());
                } else {
                    result.println("Failed to delete profile folder: " + profile.getName());
                }
            }
            if (deleteSelection.build && profile.getFolderBuild().exists()) {
                if (profile.deleteBuild()) {
                    result.println("Build folder deleted successfully: " + profile.getName());
                } else {
                    result.println("Failed to delete build folder: " + profile.getName());
                }
            }
            if (deleteSelection.assets && profile.getFileAssets().exists()) {
                if (profile.deleteAssets()) {
                    result.println("Assets file deleted successfully: " + profile.getName());
                } else {
                    result.println("Failed to delete assets file: " + profile.getName());
                }
            }
        });
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

        if (profile.hasAssetsNoConfig()) {
            System.out.println("Profile has assets only. Build files are needed to run Factorio.");
            return;
        }

        if (!profile.runFactorio()) {
            System.out.println("Failed to run Factorio with profile: " + profile.getName());
        }
    }

    private static class ExploreSelection {
        @Option(names = {"-p", "-profile"}, description = "Open the profile folder") boolean profile;
        @Option(names = {"-b", "-build"}, description = "Open the build folder") boolean build;
        @Option(names = {"-a", "-assets"}, description = "Open the assets zip") boolean assets;
    }

    @Command(name = "profile-explore", description = "Open file manager for the specified profile")
    public static void explore(
            @Parameters(arity = "1", description = "Name of the profile", paramLabel = "<PROFILE>") String name,
            @ArgGroup(exclusive = true, multiplicity = "1") ExploreSelection exploreSelection
    ) {
        Profile profile = Profile.byName(name);
        if (exploreSelection.profile && !profile.getFolderProfile().exists()) {
            System.out.println("Profile not found: " + name);
            return;
        }
        if (exploreSelection.build && !profile.getFolderBuild().exists()) {
            System.out.println("Profile build not found: " + name);
            return;
        }
        if (exploreSelection.assets && !profile.getFileAssets().exists()) {
            System.out.println("Profile assets not found: " + name);
            return;
        }
        
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(
                        exploreSelection.build ? profile.getFolderBuild() :
                        exploreSelection.assets ? profile.getFileAssets() :
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

        if (profile.hasAssetsNoConfig()) {
            System.out.println("Profile has assets only. Profile folder is needed to edit the profile configuration.");
            return;
        }

        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(profile.getFileProfileConfig());
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
            @Option(names = {"-f", "-force"}, description = "Force regeneration of the manifest, even if it already exists") boolean force
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
            @Option(names = {"-f", "-force"}, description = "Force regeneration of the manifest, even if it already exists") boolean force
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
            @Option(names = {"-f", "-force"}, description = "Force regeneration of the assets, even if they already exist") boolean force
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
            @Option(names = {"-f", "-force"}, description = "Force regeneration of all steps, even if they already exist") boolean force,
            @Option(names = {"-force-dump"}, description = "Force regeneration of factorio dump") boolean forceDump,
            @Option(names = {"-force-assets"}, description = "Force regeneration of assets") boolean forceAssets
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
            @Option(names = {"-o", "-orientation"}, description = "Orientation of the entity (0.0 - 1.0)", paramLabel = "<ORIENTATION>") Optional<Double> orientation,
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

    @Command(name = "profile-test-book", description = "Generate blueprint book with test blueprints")
    public static void testBookRender(
            @ArgGroup(exclusive = true, multiplicity = "1") ProfileSelect profileSelect
    ) {
        boolean openFolder = profileSelect.get().size() == 1;
        profileSelect.forEach((profile, result) -> {
            if (!profile.generateTestBook(openFolder)) {
                System.out.println("Failed to generate test book for profile: " + profile.getName());
            }
        });
    }
}