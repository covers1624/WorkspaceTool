/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.script.module;

import java.nio.file.Path;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Created by covers1624 on 23/05/19.
 */
public interface ModuleContainerSpec {

    default void caseSensitive(boolean caseSensitive) {
        setCaseSensitive(caseSensitive);
    }

    void setCaseSensitive(boolean caseSensitive);

    void include(String... includes);

    void exclude(String... excludes);

    boolean getCaseSensitive();

    Set<String> getIncludes();

    Set<String> getExcludes();

    Predicate<Path> createMatcher();
}
