package net.covers1624.wstool.api.workspace;

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

}
