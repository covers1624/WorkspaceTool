package net.covers1624.wt.api.impl.script.module;

import net.covers1624.wt.api.script.module.ModuleGroupSpec;
import net.covers1624.wt.api.script.module.ModuleSpec;
import net.covers1624.wt.util.PredicateCollectors;
import net.covers1624.wt.util.pattern.PatternMatcherFactory;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by covers1624 on 23/05/19.
 */
public class ModuleGroupImpl implements ModuleGroupSpec {

    private final String name;
    private final Set<String> includes = new HashSet<>();
    private final Set<String> excludes = new HashSet<>();
    private boolean caseSensitive;

    private final Map<String, ModuleSpecImpl> definedModules = new HashMap<>();

    public ModuleGroupImpl(String name) {
        this.name = name;
    }

    @Override
    public void caseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    @Override
    public void include(String... includes) {
        Collections.addAll(this.includes, includes);
    }

    @Override
    public void exclude(String... excludes) {
        Collections.addAll(this.excludes, excludes);
    }

    @Override
    public void module(String name, Consumer<ModuleSpec> consumer) {
        throw new RuntimeException("This feature is currently disabled.");
        //consumer.accept(definedModules.computeIfAbsent(name, e -> new ModuleSpecImpl()));
    }

    public Predicate<Path> createMatcher() {
        return makePredicate(includes, true).and(makePredicate(excludes, false).negate());
    }

    private Predicate<Path> makePredicate(Set<String> patterns, boolean include) {
        if (patterns.isEmpty()) {
            return include ? path -> true : path -> false;
        }
        return patterns.stream()//
                .map(pattern -> PatternMatcherFactory.getPatternMatcher(include, caseSensitive, pattern))//
                .collect(PredicateCollectors.union());
    }

}
