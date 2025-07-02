package com.demod.fbsr;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.demod.factorio.prototype.DataPrototype;
import com.demod.factorio.prototype.ItemPrototype;
import com.demod.factorio.prototype.TilePrototype;
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
                for (Profile profile : profileOrder) {
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
        Multiset<Profile> blueprintProfileCounts = HashMultiset.create();
        for (BSMetaEntity entity : blueprint.entities) {
            blueprintProfileCounts.addAll(factorioManager.lookupProfileByEntityName(entity.name));
        }
        for (BSTile tile : blueprint.tiles) {
            blueprintProfileCounts.addAll(factorioManager.lookupProfileByTileName(tile.name));
        }
        List<Profile> profileRanking = blueprintProfileCounts.entrySet().stream()
                .sorted(Comparator.comparingInt(Multiset.Entry<Profile>::getCount).reversed())
                .map(Multiset.Entry::getElement)
                .collect(Collectors.toCollection(ArrayList::new));

        //Order: <Top Profile> ==> <Vanilla> ==> <Other Profiles>
        List<Profile> profileOrder = new ArrayList<>();
        if (!profileRanking.isEmpty()) {
            profileOrder.add(profileRanking.remove(0));
        }
        profileRanking.stream().filter(Profile::isVanilla).forEach(profileOrder::add);
        profileRanking.stream().filter(p -> !p.isVanilla()).forEach(profileOrder::add);

        return byProfileOrder(factorioManager, profileOrder);
    }

    public static ModdingResolver byBlueprintBiases(FactorioManager factorioManager, BSBlueprintBook book) {
        Multiset<Profile> blueprintProfileCounts = HashMultiset.create();
        for (BSBlueprint blueprint : book.getAllBlueprints()) {
            for (BSMetaEntity entity : blueprint.entities) {
                blueprintProfileCounts.addAll(factorioManager.lookupProfileByEntityName(entity.name));
            }
            for (BSTile tile : blueprint.tiles) {
                blueprintProfileCounts.addAll(factorioManager.lookupProfileByTileName(tile.name));
            }
        }
        List<Profile> profileRanking = blueprintProfileCounts.entrySet().stream()
                .sorted(Comparator.comparingInt(Multiset.Entry<Profile>::getCount).reversed())
                .map(Multiset.Entry::getElement)
                .collect(Collectors.toCollection(ArrayList::new));

        //Order: <Top Profile> ==> <Vanilla> ==> <Other Profiles>
        List<Profile> profileOrder = new ArrayList<>();
        if (!profileRanking.isEmpty()) {
            profileOrder.add(profileRanking.remove(0));
        }
        profileRanking.stream().filter(Profile::isVanilla).forEach(profileOrder::add);
        profileRanking.stream().filter(p -> !p.isVanilla()).forEach(profileOrder::add);

        return byProfileOrder(factorioManager, profileOrder);
    }

    private final FactorioManager factorioManager;

    private ModdingResolver(FactorioManager factorioManager) {
        this.factorioManager = factorioManager;
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

    public Optional<ItemPrototype> resolveItemName(String itemName) {
        return pickPrototype(factorioManager.lookupItemByName(itemName));
    }

    public Optional<TilePrototype> resolveTileName(String name) {
        return pickPrototype(factorioManager.lookupTileByName(name));
    }

    public EntityRendererFactory resolveFactoryEntityName(String entityName) {
        return pickByPrototype(factorioManager.lookupEntityFactoryForName(entityName), EntityRendererFactory::getPrototype)
                .orElseGet(() -> factorioManager.getUnknownEntityRenderingForName(entityName));
    }

    public TileRendererFactory resolveFactoryTileName(String tileName) {
        return pickByPrototype(factorioManager.lookupTileFactoryForName(tileName), TileRendererFactory::getPrototype)
                .orElseGet(() -> factorioManager.getUnknownTileRenderingForName(tileName));
    }

    public Optional<IconDef> resolveIconQualityName(String string) {
        return pickImageDef(factorioManager.getIconManager().lookupQuality(string));
    }

    public Optional<IconDef> resolveIconItemName(String name) {
        return pickImageDef(factorioManager.getIconManager().lookupItem(name));
    }

    public Optional<? extends ImageDef> resolveIconFluidName(String name) {
        return pickImageDef(factorioManager.getIconManager().lookupFluid(name));
    }

    public Optional<TagWithQuality> resolveTag(TagToken tagToken) {
        return pickByImageDef(factorioManager.getIconManager().lookupTag(tagToken), t -> t.getDef());
    }
}
