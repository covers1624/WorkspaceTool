/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.forge.api.script;

import groovy.lang.Closure;
import net.covers1624.wt.api.script.module.ModuleSpec;
import net.covers1624.wt.util.ClosureBackedConsumer;

import java.util.function.Consumer;

/**
 * Created by covers1624 on 26/10/19.
 */
public interface Forge114ModuleSpec extends ModuleSpec {

    default void forgeMods(Closure<ModuleModsContainer> closure) {
        forgeMods(new ClosureBackedConsumer<>(closure));
    }

    void forgeMods(Consumer<ModuleModsContainer> consumer);

    ModuleModsContainer getForgeModuleModsContainer();
}
