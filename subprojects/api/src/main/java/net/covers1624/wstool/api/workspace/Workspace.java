package net.covers1624.wstool.api.workspace;

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
