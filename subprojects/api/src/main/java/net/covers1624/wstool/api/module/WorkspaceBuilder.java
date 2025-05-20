package net.covers1624.wstool.api.module;

import java.nio.file.Path;
import java.util.Map;

/**
 * A builder for a workspace of a specific type.
 * <p>
 * A workspace may choose to implement modules in any way it chooses.
 * <p>
 * Created by covers1624 on 5/3/25.
 */
public interface WorkspaceBuilder {

    /**
     * @return All top-level modules that have been created.
     */
    Map<String, Module> modules();

    /**
     * Create a new top-level module.
     *
     * @param rootDir The module's root directory.
     * @param name    The name of the module.
     * @return The module.
     */
    Module newModule(Path rootDir, String name);

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
