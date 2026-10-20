package net.covers1624.wstool.api.workspace;

import net.covers1624.wstool.gradle.api.data.SourceSetData;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Represents a source set of a module, as per Gradle terminology.
 * <p>
 * Created by covers1624 on 2/3/25.
 */
public interface SourceSet {

    /**
     * @return The name of this {@link SourceSet}.
     */
    String name();

    /**
     * @return The {@link Module} which this {@link SourceSet} is from.
     */
    Module module();

    /**
     * Returns the map of source paths for this {@link SourceSet}.
     * <p>
     * Keys are commonly 'resources', 'java', 'scala', etc.
     * <p>
     * The key 'resources' may be treated specially by a workspace implementation.
     *
     * @return The source paths for this source set.
     */
    Map<String, List<Path>> sourcePaths();

    /**
     * @return The {@link Dependency dependencies} on this source sets compile classpath.
     */
    List<Dependency> compileDependencies();

    /**
     * @return The {@link Dependency dependencies} on this source sets runtime classpath.
     */
    List<Dependency> runtimeDependencies();

    /**
     * Get the Gradle data this source set was built from.
     * <p>
     * This is used by frameworks to pull frameworks specific gradle extraction data in.
     *
     * @return The Gradle source set data that this module was built from.
     */
    SourceSetData sourceSetData();

    /**
     * Set the gradle data for this source set.
     *
     * @param data The data.
     */
    void setSourceSetData(SourceSetData data);
}
