/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.forge.api.impl;

import net.covers1624.wt.forge.api.script.Forge114ModuleSpec;
import net.covers1624.wt.forge.api.script.ModuleModsContainer;

import java.util.function.Consumer;

/**
 * Created by covers1624 on 26/10/19.
 */
public abstract class Forge114ModuleSpecTemplate implements Forge114ModuleSpec {

    private final ModuleModsContainer forgeModuleModsContainer = new ModuleModsContainerImpl();

    @Override
    public void forgeMods(Consumer<ModuleModsContainer> consumer) {
        consumer.accept(forgeModuleModsContainer);
    }

    @Override
    public ModuleModsContainer getForgeModuleModsContainer() {
        return forgeModuleModsContainer;
    }
}
