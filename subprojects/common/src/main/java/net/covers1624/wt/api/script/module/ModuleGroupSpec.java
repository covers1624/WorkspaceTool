/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.script.module;

import groovy.lang.Closure;
import net.covers1624.wt.util.ClosureBackedConsumer;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by covers1624 on 23/05/19.
 */
@Deprecated
public interface ModuleGroupSpec {

    void caseSensitive(boolean caseSensitive);

    void include(String... includes);

    void exclude(String... excludes);

    default void module(String name, Closure<ModuleSpec> closure) {
        module(name, new ClosureBackedConsumer<>(closure));
    }

    void module(String name, Consumer<ModuleSpec> consumer);

    Predicate<Path> createMatcher();
}
