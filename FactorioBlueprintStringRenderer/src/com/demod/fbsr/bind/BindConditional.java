package com.demod.fbsr.bind;

import java.util.Optional;
import java.util.function.BiPredicate;

import com.demod.fbsr.WorldMap;
import com.demod.fbsr.map.MapEntity;

public class BindConditional {
    public static final BindConditional NOOP = new BindConditional();

    @FunctionalInterface
    public interface BindConditionalTest {
        boolean check(WorldMap worldMap, MapEntity entity);
    }

    protected Optional<BindConditionalTest> conditional = Optional.empty();

    public void conditional(BindConditionalTest test) {
        this.conditional = Optional.of(test);
    }

    public boolean test(WorldMap worldMap, MapEntity entity) {
        return conditional.map(c -> c.check(worldMap, entity)).orElse(true);
    }
}
