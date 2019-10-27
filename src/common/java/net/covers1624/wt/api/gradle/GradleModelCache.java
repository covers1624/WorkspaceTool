package net.covers1624.wt.api.gradle;

import net.covers1624.wt.api.gradle.model.WorkspaceToolModel;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;

/**
 * Caches WorkspaceToolModel instances.
 *
 * Created by covers1624 on 10/7/19.
 */
public interface GradleModelCache {

    /**
     * Get a previously cached instance of {@link WorkspaceToolModel} for the given module
     * or Invoke gradle to retrieve the data again.
     *
     * @param modulePath The Path to the module.
     * @param extraHash  Any extra files to consider.
     * @return The {@link WorkspaceToolModel} instance.
     */
    WorkspaceToolModel getModel(Path modulePath, Set<String> extraHash, Set<String> extraTasks);

    default WorkspaceToolModel getModel(Path modulePath, Set<String> extraHash) {
        return getModel(modulePath, extraHash, Collections.emptySet());
    }
}
