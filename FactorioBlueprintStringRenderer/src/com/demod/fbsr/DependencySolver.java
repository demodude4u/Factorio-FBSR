package com.demod.fbsr;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONArray;

import com.demod.factorio.ModInfo;
import com.demod.factorio.ModInfo.Dependency;
import com.demod.factorio.ModInfo.DepOp;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

public class DependencySolver {

    public static class ModNameAndVersion {
        public final boolean anyVersion;
        public final String name;
        public final String version;

        public ModNameAndVersion(String name, String version) {
            anyVersion = false;
            this.name = name;
            this.version = version;
        }

        public ModNameAndVersion(String name) {
            anyVersion = true;
            this.name = name;
            this.version = null;
        }
    }

    private static class VersionConstraint {
        final DepOp op;
        final String version;
        VersionConstraint(DepOp op, String version) {
            this.op = op;
            this.version = version;
        }
        @Override
        public String toString() { return op + " " + version; }
        @Override
        public int hashCode() { return op.hashCode() * 31 + version.hashCode(); }
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof VersionConstraint)) return false;
            VersionConstraint other = (VersionConstraint) o;
            return op == other.op && version.equals(other.version);
        }
    }

    /**
     * Expands builtin set with implicit builtin dependencies.
     * Current rule: space-age implies quality and elevated-rails.
     */
    public static Set<String> expandBuiltinDependencies(Set<String> builtins) {
        Set<String> out = new LinkedHashSet<>(builtins);
        if (out.contains("space-age")) {
            out.add("quality");
            out.add("elevated-rails");
        }
        return out;
    }

    // Public existing API now delegates to internal (debug=false)
    public static Optional<List<ModInfo>> solve(List<ModNameAndVersion> required) {
        return solveInternal(required, false);
    }

    // New debug API
    public static Optional<List<ModInfo>> solveDebug(List<ModNameAndVersion> required) {
        return solveInternal(required, true);
    }

    // Core implementation with optional debug logging
    private static Optional<List<ModInfo>> solveInternal(List<ModNameAndVersion> required, boolean debug) {
        if (debug) {
            System.out.println("[DepSolver] Starting dependency resolution");
            System.out.println("[DepSolver] Roots:");
            required.forEach(r -> System.out.println("  - " + r.name + (r.anyVersion ? "" : (" = " + r.version))));
        }

        ListMultimap<String, ModInfo> modInfoLookup = ArrayListMultimap.create();
        Map<String, List<VersionConstraint>> constraints = new HashMap<>();

        for (ModNameAndVersion mnv : required) {
            if (!Profile.BUILTIN_MODS.contains(mnv.name)) {
                ensureConstraintEntry(constraints, mnv.name);
            }
            if (!mnv.anyVersion) {
                addConstraint(constraints, mnv.name, new VersionConstraint(DepOp.EQ, mnv.version));
            }
        }

        // Initial fetch
        {
            Deque<ModNameAndVersion> work = new ArrayDeque<>(required);
            while (!work.isEmpty()) {
                ModNameAndVersion mod = work.poll();
                if (modInfoLookup.containsKey(mod.name)) continue;
                if (Profile.BUILTIN_MODS.contains(mod.name)) continue;
                try {
                    List<ModInfo> allVersions = FactorioModPortal.findModAllVersions(mod.name, true);
                    modInfoLookup.putAll(mod.name, allVersions);
                    if (debug) {
                        System.out.println("[DepSolver] Fetched versions for " + mod.name + ": " +
                                allVersions.stream().map(ModInfo::getVersion).collect(Collectors.joining(", ")));
                    }
                } catch (IOException e) {
                    System.out.println("Failed to retrieve mod info for " + mod.name + ": " + e.getMessage());
                    return Optional.empty();
                }
            }
        }

        Map<String, ModInfo> selected = new HashMap<>();
        final int MAX_PASSES = 50;
        boolean changed = true;
        int pass = 0;

        while (changed && pass++ < MAX_PASSES) {
            changed = false;
            if (debug) {
                System.out.println();
                System.out.println("[DepSolver] ===== PASS " + pass + " =====");
                System.out.println("[DepSolver] Current constraints:");
                constraints.forEach((k, v) -> System.out.println("  * " + k + " -> " +
                        (v.isEmpty() ? "<any>" : v.stream().map(Object::toString).collect(Collectors.joining(" & ")))));
            }

            // Version selection
            for (String modName : new HashSet<>(constraints.keySet())) {
                if (Profile.BUILTIN_MODS.contains(modName)) continue;
                if (!modInfoLookup.containsKey(modName)) {
                    try {
                        List<ModInfo> allVersions = FactorioModPortal.findModAllVersions(modName, true);
                        modInfoLookup.putAll(modName, allVersions);
                        if (debug) {
                            System.out.println("[DepSolver] Fetched versions (late) for " + modName + ": " +
                                    allVersions.stream().map(ModInfo::getVersion).collect(Collectors.joining(", ")));
                        }
                    } catch (IOException e) {
                        System.out.println("Failed to retrieve mod info for " + modName + ": " + e.getMessage());
                        return Optional.empty();
                    }
                }
                List<ModInfo> candidates = modInfoLookup.get(modName);
                if (debug) {
                    System.out.println("[DepSolver] Evaluating " + modName + " candidates:");
                }
                ModInfo best = null;
                for (ModInfo mi : candidates) {
                    boolean ok = satisfiesAll(mi, constraints.get(modName));
                    if (debug) {
                        System.out.println("    - " + mi.getVersion() + (ok ? " [OK]" : " [FAIL]"));
                    }
                    if (ok) {
                        if (best == null || compareVersions(mi.getVersion(), best.getVersion()) > 0) {
                            best = mi;
                        }
                    }
                }
                if (best == null) {
                    System.out.println("No version of " + modName + " satisfies constraints: " + constraints.get(modName));
                    return Optional.empty();
                }
                ModInfo previous = selected.get(modName);
                if (previous == null || !previous.getVersion().equals(best.getVersion())) {
                    selected.put(modName, best);
                    changed = true;
                    if (debug) {
                        System.out.println("[DepSolver] Selected " + modName + " -> " + best.getVersion());
                    }
                } else if (debug) {
                    System.out.println("[DepSolver] Retained " + modName + " -> " + previous.getVersion());
                }
            }

            // Build new constraints from required dependencies
            Map<String, List<VersionConstraint>> newConstraints = cloneConstraintMap(constraints);
            boolean constraintsChanged = false;
            for (ModInfo mi : selected.values()) {
                for (Dependency dep : mi.getDependencies()) {
                    if (!dep.isRequired()) continue;
                    String depName = dep.getName();
                    if (Profile.BUILTIN_MODS.contains(depName)) continue;
                    ensureConstraintEntry(newConstraints, depName);
                    if (dep.getOp() != null) {
                        VersionConstraint vc = new VersionConstraint(dep.getOp(), dep.getVersion());
                        if (!newConstraints.get(depName).contains(vc)) {
                            addConstraint(newConstraints, depName, vc);
                            constraintsChanged = true;
                            if (debug) {
                                System.out.println("[DepSolver] Added constraint from " + mi.getName() +
                                        " -> " + depName + " (" + vc + ")");
                            }
                        }
                    }
                }
            }
            if (!constraintMapsEqual(constraints, newConstraints)) {
                constraints = newConstraints;
                changed = true;
            }
            if (debug && constraintsChanged == false) {
                System.out.println("[DepSolver] No new dependency constraints added this pass.");
            }

            if (!checkIncompatibilities(selected)) {
                if (debug) {
                    System.out.println("[DepSolver] Incompatibility detected. Aborting.");
                }
                return Optional.empty();
            }

            if (!changed && debug) {
                System.out.println("[DepSolver] Stable after pass " + pass + ".");
            }
        }

        if (pass >= MAX_PASSES) {
            System.out.println("Failed to stabilize dependency resolution within pass limit.");
            return Optional.empty();
        }

        // Flatten load order
        LinkedHashSet<ModInfo> ordered = new LinkedHashSet<>();
        Set<String> visiting = new HashSet<>();
        class visit {
            void go(String modName) {
                if (Profile.BUILTIN_MODS.contains(modName)) return;
                ModInfo mi = selected.get(modName);
                if (mi == null) {
                    System.out.println("Missing resolved ModInfo for " + modName);
                    throw new RuntimeException();
                }
                if (ordered.contains(mi)) return;
                if (!visiting.add(modName)) {
                    System.out.println("Cycle detected involving " + modName);
                    throw new RuntimeException();
                }
                for (ModInfo.Dependency dep : mi.getDependencies()) {
                    if (dep.isRequired()) {
                        String dn = dep.getName();
                        if (Profile.BUILTIN_MODS.contains(dn)) continue;
                        go(dn);
                    }
                }
                ordered.add(mi);
                visiting.remove(modName);
            }
        }
        visit v = new visit();
        try {
            for (ModNameAndVersion root : required) {
                if (Profile.BUILTIN_MODS.contains(root.name)) continue;
                v.go(root.name);
            }
        } catch (RuntimeException e) {
            return Optional.empty();
        }

        if (debug) {
            System.out.println();
            System.out.println("[DepSolver] Final Load Order:");
            int i = 1;
            for (ModInfo mi : ordered) {
                System.out.println(String.format("  %2d. %s %s", i++, mi.getName(), mi.getVersion()));
            }
            System.out.println();
            System.out.println("[DepSolver] Dependency Edges (required only):");
            for (ModInfo mi : ordered) {
                List<String> deps = mi.getDependencies().stream()
                        .filter(Dependency::isRequired)
                        .map(Dependency::getName)
                        .filter(n -> !Profile.BUILTIN_MODS.contains(n))
                        .collect(Collectors.toList());
                System.out.println("  " + mi.getName() + " -> " + (deps.isEmpty() ? "[]" : deps));
            }
            System.out.println("[DepSolver] Resolution complete.");
        }

        // Derive builtin roots referenced directly or via dependencies
        Set<String> builtinReferenced = new LinkedHashSet<>();
        for (ModNameAndVersion root : required) {
            if (Profile.BUILTIN_MODS.contains(root.name)) builtinReferenced.add(root.name);
        }
        for (ModInfo mi : ordered) {
            for (Dependency d : mi.getDependencies()) {
                if (Profile.BUILTIN_MODS.contains(d.getName())) builtinReferenced.add(d.getName());
            }
        }
        Set<String> expandedBuiltins = expandBuiltinDependencies(builtinReferenced);
        System.out.println();
        System.out.println("[DepSolver] Builtin referenced: " + builtinReferenced);
        if (!expandedBuiltins.equals(builtinReferenced)) {
            System.out.println("[DepSolver] Builtin expanded (implicit deps applied): " + expandedBuiltins);
        }

        return Optional.of(new ArrayList<>(ordered));
    }

    private static void addConstraint(Map<String, List<VersionConstraint>> map, String mod, VersionConstraint vc) {
        map.computeIfAbsent(mod, k -> new ArrayList<>()).add(vc);
    }
    private static void ensureConstraintEntry(Map<String, List<VersionConstraint>> map, String mod) {
        map.computeIfAbsent(mod, k -> new ArrayList<>());
    }

    private static boolean satisfiesAll(ModInfo mi, List<VersionConstraint> list) {
        if (list == null || list.isEmpty()) return true;
        String v = mi.getVersion();
        for (VersionConstraint vc : list) {
            int cmp = compareVersions(v, vc.version);
            switch (vc.op) {
                case EQ: if (cmp != 0) return false; break;
                case LT: if (!(cmp < 0)) return false; break;
                case LTE: if (!(cmp <= 0)) return false; break;
                case GT: if (!(cmp > 0)) return false; break;
                case GTE: if (!(cmp >= 0)) return false; break;
            }
        }
        return true;
    }

    private static int compareVersions(String a, String b) {
        if (a.equals(b)) return 0;
        String[] as = a.split("\\.");
        String[] bs = b.split("\\.");
        int n = Math.max(as.length, bs.length);
        for (int i = 0; i < n; i++) {
            int ai = i < as.length ? parseIntSafe(as[i]) : 0;
            int bi = i < bs.length ? parseIntSafe(bs[i]) : 0;
            if (ai != bi) return Integer.compare(ai, bi);
        }
        return 0;
    }

    private static int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

    private static boolean checkIncompatibilities(Map<String, ModInfo> selected) {
        for (ModInfo mi : selected.values()) {
            for (Dependency dep : mi.getDependencies()) {
                if (dep.isIncompatible()) {
                    ModInfo other = selected.get(dep.getName());
                    if (other == null) continue;
                    if (dep.getOp() == null) {
                        // any presence is incompatible
                        System.out.println("Incompatibility: " + mi.getName() + " incompatible with " + other.getName());
                        return false;
                    } else {
                        int cmp = compareVersions(other.getVersion(), dep.getVersion());
                        boolean conflict = false;
                        switch (dep.getOp()) {
                            case EQ: conflict = (cmp == 0); break;
                            case LT: conflict = (cmp < 0); break;
                            case LTE: conflict = (cmp <= 0); break;
                            case GT: conflict = (cmp > 0); break;
                            case GTE: conflict = (cmp >= 0); break;
                        }
                        if (conflict) {
                            System.out.println("Incompatibility: " + mi.getName() + " incompatible with " + other.getName()
                                    + " version constraint " + dep.getOp() + " " + dep.getVersion());
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private static Map<String, List<VersionConstraint>> cloneConstraintMap(Map<String, List<VersionConstraint>> src) {
        Map<String, List<VersionConstraint>> dst = new HashMap<>();
        for (Map.Entry<String, List<VersionConstraint>> e : src.entrySet()) {
            dst.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        return dst;
    }

    private static boolean constraintMapsEqual(Map<String, List<VersionConstraint>> a, Map<String, List<VersionConstraint>> b) {
        if (a.size() != b.size()) return false;
        for (String key : a.keySet()) {
            List<VersionConstraint> la = a.get(key);
            List<VersionConstraint> lb = b.get(key);
            if (lb == null) return false;
            if (la.size() != lb.size()) return false;
            // Compare as sets
            Set<VersionConstraint> sa = new HashSet<>(la);
            Set<VersionConstraint> sb = new HashSet<>(lb);
            if (!sa.equals(sb)) return false;
        }
        return true;
    }
}
