/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.dependency;

import net.covers1624.wt.api.module.Module;

/**
 * Represents a dependency between SourceSets. Either on the same module
 * or across modules.
 *
 * This is kinda pointless now, original design spec of WT was to have
 * Per-SourceSet modules like Intellij already does for projects,
 * but it was incredibly difficult to figure out how to decide
 * what source sets were what artifacts produced by gradle and then
 * used in each project. This is something im looking into for the future
 * but for now its deprecated.
 *
 * Created by covers1624 on 30/6/19.
 */
public interface SourceSetDependency extends Dependency {

    /**
     * The module the Dependency is to.
     * @return The Module.
     */
    Module getModule();

    /**
     * The Source Set the Dependency is to.
     * @return The SourceSet.
     */
    String getSourceSet();

    /**
     * Sets the Module the Dependency is to.
     * @param module The module.
     * @return The same Dependency.
     */
    SourceSetDependency setModule(Module module);

    /**
     * Sets the Source Set the Dependency is to.
     * @param sourceSet The Source Set.
     * @return The same Dependency.
     */
    SourceSetDependency setSourceSet(String sourceSet);

    /**
     * {@inheritDoc}
     */
    @Override
    SourceSetDependency setExport(boolean value);

    /**
     * {@inheritDoc}
     */
    @Override
    SourceSetDependency copy();
}
