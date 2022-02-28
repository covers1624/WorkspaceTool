/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.module;

import com.google.common.collect.Iterables;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Represents a Gradle SourceSet(kinda)
 * <p>
 * Created by covers1624 on 27/05/19.
 */
public interface SourceSet {

    /**
     * @return The name for the SourceSet.
     */
    String getName();

    /**
     * @return A List of Paths containing all the resources provided by this SourceSet.
     */
    List<Path> getResources();

    /**
     * Adds a Path to this SourceSets Resources.
     *
     * @param path The Resource.
     */
    void addResource(Path path);

    /**
     * Replaces this SourceSets resources with the provided List.
     *
     * @param paths The Resources.
     */
    void setResources(List<Path> paths);

    /**
     * Represents a Map of Language -> Sources
     *
     * @return Returns a Map of Language to Source list Map.
     */
    Map<String, List<Path>> getSourceMap();

    /**
     * Add a Languages Sources to this SourceSet
     * Replaces any existing Sources
     *
     * @param name  The language name.
     * @param paths The Paths.
     */
    void setSource(String name, List<Path> paths);

    void addSource(String name, List<Path> paths);

    void addSource(String name, Path path);

    /**
     * Sets all Sources for this SourceSet.
     *
     * @param sourceMap The Map.
     */
    void setSourceMap(Map<String, List<Path>> sourceMap);

    /**
     * @return Returns an Iterable with access to all SourceSet sources.
     */
    default Iterable<Path> getAllSource() {
        return Iterables.concat(getSourceMap().values());
    }

    default Iterable<Path> getAllSourceResource() {
        return Iterables.concat(getAllSource(), getResources());
    }

    /**
     * @return The Compile Configuration for this SourceSet.
     */
    Configuration getCompileConfiguration();

    /**
     * Sets the Compile Configuration for this SourceSet.
     *
     * @param configuration The Configuration.
     */
    void setCompileConfiguration(Configuration configuration);

    /**
     * @return The Runtime Configuration for this SourceSet.
     */
    Configuration getRuntimeConfiguration();

    /**
     * Sets the Runtime Configuration for this SourceSet.
     *
     * @param configuration The Configuration.
     */
    void setRuntimeConfiguration(Configuration configuration);

    /**
     * @return The CompileOnly Configuration for this SourceSet.
     */
    Configuration getCompileOnlyConfiguration();

    /**
     * Sets the CompileOnly Configuration for this SourceSet.
     *
     * @param configuration The Configuration.
     */
    void setCompileOnlyConfiguration(Configuration configuration);
}
