package com.demod.fbsr.cli;

import java.util.Optional;
import java.util.function.Function;

import org.json.JSONObject;

import com.demod.factorio.prototype.DataPrototype;
import com.demod.fbsr.FactorioManager;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "lua", description = "Factorio Lua related commands")
public class CmdLua {

    public static enum PrototypeTypes {
        entity(FactorioManager::lookupEntityByName),
        equipment(FactorioManager::lookupEquipmentByName),
        fluid(FactorioManager::lookupFluidByName),
        item(FactorioManager::lookupItemByName),
        itemgroup(FactorioManager::lookupItemGroupByName),
        recipe(FactorioManager::lookupRecipeByName),
        technology(FactorioManager::lookupTechnologyByName),
        tile(FactorioManager::lookupTileByName)
        ;
        private Function<String, Optional<? extends DataPrototype>> lookupFunction;

        private PrototypeTypes(Function<String, Optional<? extends DataPrototype>> lookupFunction) {
            this.lookupFunction = lookupFunction;
        }
    }

    @Command(name = "prototype", description = "Query prototypes")
    public void lookupPrototype(
            @Parameters(description = "Type of prototype to query") PrototypeTypes type,
            @Parameters(description = "Name of the prototype to query") String name
    ) {
        //TODO
    }

    @Command(name = "raw", description = "Query data.raw")
    public void lookupRaw(
            @Parameters(description = "Lua path") String path
    ) {
        //TODO
    }

}
