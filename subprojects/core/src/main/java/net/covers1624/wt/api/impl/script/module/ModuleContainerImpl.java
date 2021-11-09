/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.impl.script.module;

import net.covers1624.wt.api.mixin.MixinInstantiator;
import net.covers1624.wt.api.script.module.ModuleContainerSpec;
import net.covers1624.wt.util.pattern.PatternMatcherFactory;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by covers1624 on 23/05/19.
 */
public class ModuleContainerImpl implements ModuleContainerSpec {

    private final Set<String> includes = new HashSet<>();
    private final Set<String> excludes = new HashSet<>();
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
    public Predicate<Path> createMatcher() {
        return makePredicate(includes, true).and(makePredicate(excludes, false).negate());
    }

    private Predicate<Path> makePredicate(Set<String> patterns, boolean include) {
        if (patterns.isEmpty()) {
            return include ? path -> true : path -> false;
        }
        List<Predicate<Path>> predicates = patterns.stream()
                .map(pattern -> PatternMatcherFactory.getPatternMatcher(include, caseSensitive, pattern))
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
    }

}
