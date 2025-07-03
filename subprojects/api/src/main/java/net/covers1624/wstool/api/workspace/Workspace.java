package net.covers1624.wstool.api.workspace;

import net.covers1624.wstool.api.workspace.runs.RunConfig;

import java.nio.file.Path;
import java.util.Map;

/**
 * Represents an abstract interface into a Workspace implementation.
 * <p>
 * Created by covers1624 on 5/3/25.
 */
public interface Workspace {

    /**
     * @return All top-level modules that have been created.
     */
    Map<String, ? extends Module> modules();

    /**
     * Create a new top-level module.
     *
     * @param rootDir The module's root directory.
     * @param name    The name of the module.
     * @return The module.
     */
    Module newModule(Path rootDir, String name);

    /**
     * @return All run configurations that have been created.
     */
    Map<String, ? extends RunConfig> runConfigs();

    /**
     * Create a new run configuration for this project.
     *
     * @param name The name. If denoted with slashes, may indicate folders
     *             or grouping. This is workspace dependent. Some may only
     *             support single levels of nesting, others may support more
     *             or none. The key passed in here is used verbatim as its
     *             identity/lookup key.
     * @return The new run config.
     */
    RunConfig newRunConfig(String name);

    /**
     * Set the Java version for the workspace.
     * <p>
     * If not set, the workspace will default to Java 8.
     *
     * @param version The version.
     */
    void setJavaVersion(int version);

    /**
     * Write the workspace to disk.
     */
    void writeWorkspace();
}
