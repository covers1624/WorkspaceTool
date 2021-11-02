/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.gradle;

import net.covers1624.wt.api.gradle.model.WorkspaceToolModel;

import java.nio.file.Path;
import java.util.Set;

/**
 * Caches WorkspaceToolModel instances.
 * <p>
 * Created by covers1624 on 10/7/19.
 */
public interface GradleModelCache {

    /**
     * Get a previously cached instance of {@link WorkspaceToolModel} for the given module
     * or Invoke gradle to retrieve the data again.
     *
     * @param modulePath The Path to the module.
     * @param extraHash  Any extra files to consider.
     * @param extraTasks Any extra tasks to execute before extracting the Model.
     * @return The {@link WorkspaceToolModel} instance.
     */
    WorkspaceToolModel getModel(Path modulePath, Set<String> extraHash, Set<String> extraTasks);
}
