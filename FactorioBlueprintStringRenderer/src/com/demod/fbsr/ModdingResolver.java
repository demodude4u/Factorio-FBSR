package com.demod.fbsr;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.demod.factorio.prototype.DataPrototype;
import com.demod.fbsr.bs.BSBlueprint;
import com.demod.fbsr.bs.BSMetaEntity;
import com.demod.fbsr.bs.BSTile;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

public abstract class ModdingResolver {

    public static ModdingResolver byProfileOrder(FactorioManager factorioManager, Profile... profileOrder) {
        return new ModdingResolver() {
            @Override
            public <T extends DataPrototype> Optional<T> resolvePrototype(List<T> prototypes) {
                List<Profile> protoProfiles = prototypes.stream().map(p -> factorioManager.lookupProfileByData(p.getTable().getData())).collect(Collectors.toList());
                for (Profile profile : profileOrder) {
                    for (int i=0;i<protoProfiles.size();i++) {
                        if (profile.equals(protoProfiles.get(i))) {
                            return Optional.of(prototypes.get(i));
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

        //TODO
    }

    public abstract <T extends DataPrototype> Optional<T> resolvePrototype(List<T> prototypes);
}
