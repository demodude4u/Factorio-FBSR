package com.demod.fbsr.cli;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.json.JSONException;
import org.json.JSONObject;

import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.prototype.DataPrototype;
import com.demod.fbsr.FactorioManager;
import com.demod.fbsr.Profile;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = " ", description = "Factorio Dump related commands")
public class CmdDump {

    @Command(name = "dump-entity", description = "Dump an entity prototype from factorio data")
    public static void dumpEntity(
            @Option(names = "-profile", description = "Load from profile (default vanilla)", defaultValue = "vanilla", paramLabel = "<PROFILE>") String profileName,
            @Parameters(arity = "1", description = "Prototype name", paramLabel="PROTOTYPE") String protoName
    ) {
        dumpProto(profileName, protoName, "entity", DataTable::getEntity);
    }

    @Command(name = "dump-item", description = "Dump an item prototype from factorio data")
    public static void dumpItem(
            @Option(names = "-profile", description = "Load from profile (default vanilla)", defaultValue = "vanilla", paramLabel = "<PROFILE>") String profileName,
            @Parameters(arity = "1", description = "Prototype name", paramLabel="PROTOTYPE") String protoName
    ) {
        dumpProto(profileName, protoName, "item", DataTable::getItem);
    }

    @Command(name = "dump-recipe", description = "Dump a recipe prototype from factorio data")
    public static void dumpRecipe(
            @Option(names = "-profile", description = "Load from profile (default vanilla)", defaultValue = "vanilla", paramLabel = "<PROFILE>") String profileName,
            @Parameters(arity = "1", description = "Prototype name", paramLabel="PROTOTYPE") String protoName
    ) {
        dumpProto(profileName, protoName, "recipe", DataTable::getRecipe);
    }

    @Command(name = "dump-fluid", description = "Dump a fluid prototype from factorio data")
    public static void dumpFluid(
            @Option(names = "-profile", description = "Load from profile (default vanilla)", defaultValue = "vanilla", paramLabel = "<PROFILE>") String profileName,
            @Parameters(arity = "1", description = "Prototype name", paramLabel="PROTOTYPE") String protoName
    ) {
        dumpProto(profileName, protoName, "fluid", DataTable::getFluid);
    }

    @Command(name = "dump-technology", description = "Dump a technology prototype from factorio data")
    public static void dumpTechnology(
            @Option(names = "-profile", description = "Load from profile (default vanilla)", defaultValue = "vanilla", paramLabel = "<PROFILE>") String profileName,
            @Parameters(arity = "1", description = "Prototype name", paramLabel="PROTOTYPE") String protoName
    ) {
        dumpProto(profileName, protoName, "technology", DataTable::getTechnology);
    }

    @Command(name = "dump-equipment", description = "Dump an equipment prototype from factorio data")
    public static void dumpEquipment(
            @Option(names = "-profile", description = "Load from profile (default vanilla)", defaultValue = "vanilla", paramLabel = "<PROFILE>") String profileName,
            @Parameters(arity = "1", description = "Prototype name", paramLabel="PROTOTYPE") String protoName
    ) {
        dumpProto(profileName, protoName, "equipment", DataTable::getEquipment);
    }

    @Command(name = "dump-tile", description = "Dump a tile prototype from factorio data")
    public static void dumpTile(
            @Option(names = "-profile", description = "Load from profile (default vanilla)", defaultValue = "vanilla", paramLabel = "<PROFILE>") String profileName,
            @Parameters(arity = "1", description = "Prototype name", paramLabel="PROTOTYPE") String protoName
    ) {
        dumpProto(profileName, protoName, "tile", DataTable::getTile);
    }

    private static void dumpProto(String profileName, String protoName, String type, BiFunction<DataTable, String, Optional<? extends DataPrototype>> prototypeGetter) {
        Profile profile = Profile.byName(profileName);
        if (!profile.isValid()) {
            System.out.println("Profile not found or invalid: " + profileName);
            return;
        }

        FactorioData factorioData;
        if (profile.hasAssets()) {
            factorioData = new FactorioData(profile.getFileAssets());
        } else if (profile.hasDump()) {
            factorioData = new FactorioData(profile.getFileDumpDataJson(), profile.getDumpFactorioVersion());
        } else {
            System.out.println("Profile does not have assets or factorio dump. Please run 'build-dump " + profile.getName() + "' first.");
            return;
        }

        if (!factorioData.initialize(false)) {
            System.out.println("Failed to initialize factorio data for profile: " + profileName);
            return;
        }

        DataTable table = factorioData.getTable();
        Optional<? extends DataPrototype> prototype = prototypeGetter.apply(table, protoName);

        if (prototype.isEmpty()) {
            System.out.println("Prototype " + type + " not found: " + protoName);
            return;
        }

        File folderBuildDebug = new File(profile.getFolderBuild(), "debug");
        folderBuildDebug.mkdirs();

        JSONObject jsonProto = (JSONObject) prototype.get().lua().getJson();
        File fileProto = new File(folderBuildDebug, profile.getName() + " " + type + " " + protoName + " " + factorioData.getVersion() + ".json");

        try {
            Files.writeString(fileProto.toPath(), jsonProto.toString(2));
            Desktop.getDesktop().open(fileProto);
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            System.out.println("Failed to write prototype to file: " + fileProto.getAbsolutePath());
            return;
        }
    }

    @Command(name = "dump-raw", description = "Query data.raw")
    public static void lookupRaw(
            @Option(names = {"-p", "-profile"}, description = "Load from profile (default vanilla)", defaultValue = "vanilla", paramLabel = "<PROFILE>") String profileName,
            @Parameters(arity = "1", description = "Lua path", paramLabel = "<PATH>") String path
    ) {
        Profile profile = Profile.byName(profileName);
        if (!profile.isValid()) {
            System.out.println("Profile not found or invalid: " + profileName);
            return;
        }

        FactorioData factorioData;
        if (profile.hasAssets()) {
            factorioData = new FactorioData(profile.getFileAssets());
        } else if (profile.hasDump()) {
            factorioData = new FactorioData(profile.getFileDumpDataJson(), profile.getDumpFactorioVersion());
        } else {
            System.out.println("Profile does not have assets or factorio dump. Please run 'build-dump " + profile.getName() + "' first.");
            return;
        }

        if (!factorioData.initialize(false)) {
            System.out.println("Failed to initialize factorio data for profile: " + profileName);
            return;
        }

        DataTable table = factorioData.getTable();
        String[] pathParts = path.split("[./\\\\]+");
        Optional<LuaValue> optLua = table.getRaw(pathParts);
        if (optLua.isEmpty()) {
            System.out.println("Lua value not found for data.raw path: " + Arrays.toString(pathParts));
            return;
        }
        String jsonString;
        Object value = optLua.get().getJson();
        if (value instanceof JSONObject) {
            jsonString = ((JSONObject) value).toString(2);
        } else if (value instanceof JSONArray) {
            jsonString = ((JSONArray) value).toString(2);
        } else {
            System.out.println();
            System.out.println(path+ " = " + value);
            return;
        }

        File folderBuildDebug = new File(profile.getFolderBuild(), "debug");
        folderBuildDebug.mkdirs();

        String pathEncoded = String.join("_", pathParts).replaceAll("[^a-zA-Z0-9_\\-]", "_");
        int maxLength = 100;
        if (pathEncoded.length() > maxLength) {
            pathEncoded = pathEncoded.substring(0, maxLength);
        }
        File fileProto = new File(folderBuildDebug, profile.getName() + " raw " + pathEncoded + " " + factorioData.getVersion() + ".json");

        try {
            Files.writeString(fileProto.toPath(), jsonString);
            Desktop.getDesktop().open(fileProto);
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            System.out.println("Failed to write prototype to file: " + fileProto.getAbsolutePath());
            return;
        }
    }

}
