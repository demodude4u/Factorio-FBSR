package com.demod.fbsr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.demod.factorio.prototype.DataPrototype;
import com.demod.factorio.prototype.ItemPrototype;
import com.demod.factorio.prototype.RecipePrototype;
import com.demod.factorio.prototype.TilePrototype;
import com.demod.fbsr.Profile.ManifestModInfo;
import com.demod.fbsr.RichText.TagToken;
import com.demod.fbsr.bs.BSBlueprint;
import com.demod.fbsr.bs.BSBlueprintBook;
import com.demod.fbsr.bs.BSMetaEntity;
import com.demod.fbsr.bs.BSTile;
import com.demod.fbsr.def.IconDef;
import com.demod.fbsr.def.ImageDef;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

public abstract class ModdingResolver {

    public static ModdingResolver byProfileOrder(FactorioManager factorioManager, List<Profile> profileOrder, boolean includeAllProfiles) {
        List<Profile> profileRanking = new ArrayList<>(profileOrder);

        if (includeAllProfiles) {
            profileRanking.addAll(factorioManager.getProfiles().stream().filter(p -> !profileRanking.contains(p)).collect(Collectors.toList()));
        }
        
        return new ModdingResolver(factorioManager) {
            @Override
            public <T> Optional<T> pickByProfile(List<T> items, Function<T, Profile> profileMapper) {
                List<Profile> protoProfiles = items.stream().map(profileMapper).collect(Collectors.toList());
                for (Profile profile : profileRanking) {
                    for (int i=0;i<protoProfiles.size();i++) {
                        if (profile.equals(protoProfiles.get(i))) {
                            return Optional.of(items.get(i));
                        }
                    }
                }
                return Optional.empty();
            }
        };
    }

    public static ModdingResolver byBlueprintBiases(FactorioManager factorioManager, BSBlueprint blueprint) {
        Set<String> entityNames = new LinkedHashSet<>();
        Set<String> tileNames = new LinkedHashSet<>();

        for (BSMetaEntity entity : blueprint.entities) {
            List<EntityRendererFactory> factories = factorioManager.lookupEntityFactoryForName(entity.name);
            for (EntityRendererFactory factory : factories) {
                entityNames.add(entity.name);
            }
        }
        for (BSTile tile : blueprint.tiles) {
            List<TileRendererFactory> factories = factorioManager.lookupTileFactoryForName(tile.name);
            for (TileRendererFactory factory : factories) {
                tileNames.add(tile.name);
            }
        }

        return byNameBiases(factorioManager, entityNames, tileNames);
    }

    public static ModdingResolver byBlueprintBiases(FactorioManager factorioManager, BSBlueprintBook book) {
        Set<String> entityNames = new LinkedHashSet<>();
        Set<String> tileNames = new LinkedHashSet<>();
        
        for (BSBlueprint blueprint : book.getAllBlueprints()) {
            for (BSMetaEntity entity : blueprint.entities) {
                List<EntityRendererFactory> factories = factorioManager.lookupEntityFactoryForName(entity.name);
                for (EntityRendererFactory factory : factories) {
                    entityNames.add(entity.name);
                }
            }
            for (BSTile tile : blueprint.tiles) {
                List<TileRendererFactory> factories = factorioManager.lookupTileFactoryForName(tile.name);
                for (TileRendererFactory factory : factories) {
                    tileNames.add(tile.name);
                }
            }
        }

        return byNameBiases(factorioManager, entityNames, tileNames);
    }

    public static ModdingResolver byNameBiases(FactorioManager factorioManager, Collection<String> entityNames, Collection<String> tileNames) {
        List<ManifestModInfo> mods = new ArrayList<>();
        List<List<ManifestModInfo>> modClashes = new ArrayList<>();
        
        for (String entityName : entityNames) {
            List<EntityRendererFactory> factories = factorioManager.lookupEntityFactoryForName(entityName);
            
            if (factories.stream().anyMatch(f -> f.getProfile().isVanilla())) {
                continue;
            }
            
            if (factories.size() == 1) {
                mods.addAll(factories.get(0).getMods());
                continue;
            }

            List<ManifestModInfo> factoryMods = factories.stream()
                    .flatMap(f -> f.getMods().stream())
                    .collect(Collectors.toList());
            modClashes.add(factoryMods);
        }

        for (String tileName : tileNames) {
            List<TileRendererFactory> factories = factorioManager.lookupTileFactoryForName(tileName);
            
            if (factories.stream().anyMatch(f -> f.getProfile().isVanilla())) {
                continue;
            }
            
            if (factories.size() == 1) {
                mods.addAll(factories.get(0).getMods());
                continue;
            }

            List<ManifestModInfo> factoryMods = factories.stream()
                    .flatMap(f -> f.getMods().stream())
                    .collect(Collectors.toList());
            modClashes.add(factoryMods);
        }

        for (List<ManifestModInfo> clash : modClashes) {
            if (clash.stream().noneMatch(mods::contains)) {
                mods.addAll(clash);
            }
        }

        mods.sort(Comparator.<ManifestModInfo>comparingLong(m -> m.downloads).reversed());
        List<Profile> modProfiles = mods.stream()
                .map(ManifestModInfo::getProfile)
                .distinct()
                .collect(Collectors.toList());

        List<Profile> profileOrder = new ArrayList<>(modProfiles);
        profileOrder.add(factorioManager.getProfileVanilla());
        return byProfileOrder(factorioManager, profileOrder, true);
    }

    private final FactorioManager factorioManager;

    private ModdingResolver(FactorioManager factorioManager) {
        this.factorioManager = factorioManager;
    }

    public FactorioManager getFactorioManager() {
        return factorioManager;
    }

    public abstract <T> Optional<T> pickByProfile(List<T> items, Function<T, Profile> profileMapper);

    public <T> Optional<T> pickByPrototype(List<T> items, Function<T, ? extends DataPrototype> prototypeMapper) {
        return pickByProfile(items, item -> factorioManager.lookupProfileByData(prototypeMapper.apply(item).getTable().getData()));
    }
    
    public <T extends DataPrototype> Optional<T> pickPrototype(List<T> prototypes) {
        return pickByPrototype(prototypes, Function.identity());
    }

    public <T> Optional<T> pickByImageDef(List<T> items, Function<T, ? extends ImageDef> defMapper) {
        return pickByProfile(items, item -> defMapper.apply(item).getProfile());
    }

    public <T extends ImageDef> Optional<T> pickImageDef(List<T> imageDefs) {
        return pickByImageDef(imageDefs, Function.identity());
    }

    //////////////////////////////////
    /// Prototypes
    //////////////////////////////////

    public Optional<ItemPrototype> resolveItemName(String itemName) {
        return pickPrototype(factorioManager.lookupItemByName(itemName));
    }

    public Optional<TilePrototype> resolveTileName(String name) {
        return pickPrototype(factorioManager.lookupTileByName(name));
    }

    public Optional<RecipePrototype> resolveRecipeName(String name) {
        return pickPrototype(factorioManager.lookupRecipeByName(name));
    }

    //////////////////////////////////
    /// Factories
    //////////////////////////////////

    public EntityRendererFactory resolveFactoryEntityName(String entityName) {
        return pickByPrototype(factorioManager.lookupEntityFactoryForName(entityName), EntityRendererFactory::getPrototype)
                .orElseGet(() -> factorioManager.getUnknownEntityRenderingForName(entityName));
    }

    public TileRendererFactory resolveFactoryTileName(String tileName) {
        return pickByPrototype(factorioManager.lookupTileFactoryForName(tileName), TileRendererFactory::getPrototype)
                .orElseGet(() -> factorioManager.getUnknownTileRenderingForName(tileName));
    }

    //////////////////////////////////
    /// Icons
    //////////////////////////////////

    public Optional<IconDef> resolveIconQualityName(String string) {
        return pickImageDef(factorioManager.getIconManager().lookupQuality(string));
    }

    public Optional<IconDef> resolveIconItemName(String name) {
        return pickImageDef(factorioManager.getIconManager().lookupItem(name));
    }

    public Optional<IconDef> resolveIconFluidName(String name) {
        return pickImageDef(factorioManager.getIconManager().lookupFluid(name));
    }

    public Optional<IconDef> resolveIconRecipeName(String string) {
        return pickImageDef(factorioManager.getIconManager().lookupRecipe(string));
    }

    public Optional<IconDef> resolveIconEntityName(String name) {
        return pickImageDef(factorioManager.getIconManager().lookupEntity(name));
    }

    public Optional<IconDef> resolveIconAsteroidChunkName(String name) {
        return pickImageDef(factorioManager.getIconManager().lookupAsteroidChunk(name));
    }

    //////////////////////////////////
    /// Others
    //////////////////////////////////

    public Optional<TagWithQuality> resolveTag(TagToken tagToken) {
        return pickByImageDef(factorioManager.getIconManager().lookupTag(tagToken), t -> t.getDef());
    }

    public Optional<IconDefWithQuality> resolveSignalID(String type, String name, Optional<String> quality) {
        return pickByImageDef(factorioManager.getIconManager().lookupSignalID(type, name, quality), i -> i.getDef());
    }

    public Optional<IconDefWithQuality> resolveFilter(Optional<String> type, Optional<String> name, Optional<String> quality) {
        return pickByImageDef(factorioManager.getIconManager().lookupFilter(type, name, quality), i -> i.getDef());
    }
}
