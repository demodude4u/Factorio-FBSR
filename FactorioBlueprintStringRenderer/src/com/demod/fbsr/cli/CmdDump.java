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

@Command(name = "dump", description = "Factorio Dump related commands")
public class CmdDump {

    @Command(name = "entity", description = "Dump an entity prototype from factorio data")
    public void dumpEntity(
            @Option(names = "-profile", description = "Load from profile (default vanilla)", defaultValue = "vanilla") String profileName,
            @Parameters(arity = "1", description = "Prototype name") String protoName
    ) {
        dumpProto(profileName, protoName, "entity", DataTable::getEntity);
    }

    @Command(name = "item", description = "Dump an item prototype from factorio data")
    public void dumpItem(
            @Option(names = "-profile", description = "Load from profile (default vanilla)", defaultValue = "vanilla") String profileName,
            @Parameters(arity = "1", description = "Prototype name") String protoName
    ) {
        dumpProto(profileName, protoName, "item", DataTable::getItem);
    }

    @Command(name = "recipe", description = "Dump a recipe prototype from factorio data")
    public void dumpRecipe(
            @Option(names = "-profile", description = "Load from profile (default vanilla)", defaultValue = "vanilla") String profileName,
            @Parameters(arity = "1", description = "Prototype name") String protoName
    ) {
        dumpProto(profileName, protoName, "recipe", DataTable::getRecipe);
    }

    @Command(name = "fluid", description = "Dump a fluid prototype from factorio data")
    public void dumpFluid(
            @Option(names = "-profile", description = "Load from profile (default vanilla)", defaultValue = "vanilla") String profileName,
            @Parameters(arity = "1", description = "Prototype name") String protoName
    ) {
        dumpProto(profileName, protoName, "fluid", DataTable::getFluid);
    }

    @Command(name = "technology", description = "Dump a technology prototype from factorio data")
    public void dumpTechnology(
            @Option(names = "-profile", description = "Load from profile (default vanilla)", defaultValue = "vanilla") String profileName,
            @Parameters(arity = "1", description = "Prototype name") String protoName
    ) {
        dumpProto(profileName, protoName, "technology", DataTable::getTechnology);
    }

    @Command(name = "equipment", description = "Dump an equipment prototype from factorio data")
    public void dumpEquipment(
            @Option(names = "-profile", description = "Load from profile (default vanilla)", defaultValue = "vanilla") String profileName,
            @Parameters(arity = "1", description = "Prototype name") String protoName
    ) {
        dumpProto(profileName, protoName, "equipment", DataTable::getEquipment);
    }

    @Command(name = "tile", description = "Dump a tile prototype from factorio data")
    public void dumpTile(
            @Option(names = "-profile", description = "Load from profile (default vanilla)", defaultValue = "vanilla") String profileName,
            @Parameters(arity = "1", description = "Prototype name") String protoName
    ) {
        dumpProto(profileName, protoName, "tile", DataTable::getTile);
    }

    private void dumpProto(String profileName, String protoName, String type, BiFunction<DataTable, String, Optional<? extends DataPrototype>> prototypeGetter) {
        Profile profile = Profile.byName(profileName);
        if (!profile.isValid()) {
            System.out.println("Profile not found or invalid: " + profileName);
            return;
        }

        File dataFile;
        if (profile.hasData()) {
            dataFile = profile.getFileFactorioData();
        } else if (profile.hasDump()) {
            dataFile = profile.getFileDumpData();
        } else {
            System.out.println("Profile does not have factorio data or dump. Please run 'profile build-dump' first.");
            return;
        }

        FactorioData factorioData = new FactorioData(dataFile);
        if (!factorioData.initialize(false)) {
            System.out.println("Failed to initialize factorio data from file: " + dataFile.getAbsolutePath());
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

    @Command(name = "raw", description = "Query data.raw")
    public void lookupRaw(
            @Parameters(description = "Lua path") String path
    ) {
        //TODO
    }

}
