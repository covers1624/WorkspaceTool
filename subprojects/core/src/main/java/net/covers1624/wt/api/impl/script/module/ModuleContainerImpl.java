package net.covers1624.wt.api.impl.script.module;

import net.covers1624.wt.api.mixin.MixinInstantiator;
import net.covers1624.wt.api.script.module.ModuleContainerSpec;
import net.covers1624.wt.api.script.module.ModuleSpec;
import net.covers1624.wt.util.PredicateCollectors;
import net.covers1624.wt.util.pattern.PatternMatcherFactory;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by covers1624 on 23/05/19.
 */
public class ModuleContainerImpl implements ModuleContainerSpec {

    private final Set<String> includes = new HashSet<>();
    private final Set<String> excludes = new HashSet<>();
    private final Map<String, ModuleSpec> customModules = new HashMap<>();
    private boolean caseSensitive;

    private final MixinInstantiator mixinInstantiator;

    public ModuleContainerImpl(MixinInstantiator mixinInstantiator) {
        this.mixinInstantiator = mixinInstantiator;
    }

    @Override
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    @Override
    public void include(String... includes) {
        Collections.addAll(this.includes, includes);
    }

    @Override
    public void include(String include, Consumer<ModuleSpec> consumer) {
        include(include);
        ModuleSpec spec = customModules.get(include);
        if (spec == null) {
            spec = mixinInstantiator.instantiate(ModuleSpec.class);
            customModules.put(include, spec);
        }
        consumer.accept(spec);
    }

    @Override
    public void exclude(String... excludes) {
        Collections.addAll(this.excludes, excludes);
    }

    @Override
    public boolean getCaseSensitive() {
        return caseSensitive;
    }

    @Override
    public Set<String> getIncludes() {
        return Collections.unmodifiableSet(includes);
    }

    @Override
    public Set<String> getExcludes() {
        return Collections.unmodifiableSet(excludes);
    }

    @Override
    public Map<String, ModuleSpec> getCustomModules() {
        return customModules;
    }

    @Override
    public Predicate<Path> createMatcher() {
        return makePredicate(includes, true).and(makePredicate(excludes, false).negate());
    }

    private Predicate<Path> makePredicate(Set<String> patterns, boolean include) {
        if (patterns.isEmpty()) {
            return include ? path -> true : path -> false;
        }
        List<Predicate<Path>> predicates = patterns.stream()//
                .map(pattern -> PatternMatcherFactory.getPatternMatcher(include, caseSensitive, pattern))//
                .collect(Collectors.toList());
        if (predicates.size() == 1) {
            return predicates.get(0);
        } else {
            Predicate<Path> predicate = predicates.get(0);
            for (int i = 1; i < predicates.size(); i++) {
                predicate = predicate.or(predicates.get(i));

            }
            return predicate;
        }
//        return patterns.stream()//
//                .map(pattern -> PatternMatcherFactory.getPatternMatcher(include, caseSensitive, pattern))//
//                .collect(PredicateCollectors.union());
    }

}
