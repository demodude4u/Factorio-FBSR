package com.demod.fbsr;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.demod.factorio.prototype.DataPrototype;
import com.demod.factorio.prototype.ItemPrototype;
import com.demod.fbsr.bs.BSBlueprint;
import com.demod.fbsr.bs.BSMetaEntity;
import com.demod.fbsr.bs.BSTile;
import com.demod.fbsr.def.IconDef;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

public abstract class ModdingResolver {

    public static ModdingResolver byProfileOrder(FactorioManager factorioManager, List<Profile> profileOrder) {
        return new ModdingResolver(factorioManager) {
            @Override
            public <T> Optional<T> pick(List<T> items, Function<T, ? extends DataPrototype> prototypeMapper) {
                List<Profile> protoProfiles = items.stream().map(item -> factorioManager.lookupProfileByData(prototypeMapper.apply(item).getTable().getData())).collect(Collectors.toList());
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
        List<Profile> profiles = factorioManager.getProfiles();

        Multimap<String, Profile> entityProfiles = HashMultimap.create();
        Multimap<String, Profile> tileProfiles = HashMultimap.create();
        for (Profile profile : profiles) {
            RenderingRegistry renderingRegistry = profile.getRenderingRegistry();
            for (EntityRendererFactory factory : renderingRegistry.getEntityFactories()) {
                entityProfiles.put(factory.getName(), profile);
            }
            for (TileRendererFactory factory : renderingRegistry.getTileFactories()) {
                tileProfiles.put(factory.getName(), profile);
            }
        }
        
        Multiset<Profile> blueprintProfileCounts = HashMultiset.create();
        for (BSMetaEntity entity : blueprint.entities) {
            blueprintProfileCounts.addAll(entityProfiles.get(entity.name));
        }
        for (BSTile tile : blueprint.tiles) {
            blueprintProfileCounts.addAll(tileProfiles.get(tile.name));
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

    public abstract <T> Optional<T> pick(List<T> items, Function<T, ? extends DataPrototype> prototypeMapper);
    
    public <T extends DataPrototype> Optional<T> pickPrototype(List<T> prototypes) {
        return pick(prototypes, Function.identity());
    }

    public Optional<ItemPrototype> resolveItemName(String itemName) {
        return pick(factorioManager.lookupItemByName(itemName), Function.identity());
    }

    public EntityRendererFactory resolveFactoryEntityName(String entityName) {
        return pick(factorioManager.lookupEntityFactoryForName(entityName), EntityRendererFactory::getPrototype)
                .orElseGet(() -> factorioManager.getUnknownEntityRenderingForName(entityName));
    }

    public TileRendererFactory resolveFactoryTileName(String tileName) {
        return pick(factorioManager.lookupTileFactoryForName(tileName), TileRendererFactory::getPrototype)
                .orElseGet(() -> factorioManager.getUnknownTileRenderingForName(tileName));
    }

    public Optional<IconDef> resolveIconQualityName(String string) {
        return pick(factorioManager.getIconManager().lookupQuality(string), IconDef::getPrototype);
    }

}
