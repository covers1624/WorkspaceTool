/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.impl.script.module;

import net.covers1624.wt.api.script.module.ModuleContainerSpec;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by covers1624 on 23/05/19.
 */
public class ModuleContainerImpl implements ModuleContainerSpec {

    private final Set<String> includes = new HashSet<>();

    public ModuleContainerImpl() {
    }

    @Override
    public void include(String... includes) {
        Collections.addAll(this.includes, includes);
    }

    @Override
    public Set<String> getIncludes() {
        return Collections.unmodifiableSet(includes);
    }
}
