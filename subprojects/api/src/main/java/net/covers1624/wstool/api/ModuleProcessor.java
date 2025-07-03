package net.covers1624.wstool.api;

import net.covers1624.wstool.api.workspace.Dependency;
import net.covers1624.wstool.api.workspace.Module;
import net.covers1624.wstool.api.workspace.Workspace;
import net.covers1624.wstool.gradle.api.data.ConfigurationData;

import java.nio.file.Path;
import java.util.Set;

/**
 * Interface for modules to call into core WorkspaceTool builder operations.
 * <p>
 * Created by covers1624 on 5/28/25.
 */
public interface ModuleProcessor {

    /**
     * Extract a project and build a {@link Module} for it in the given {@link Workspace}.
     *
     * @param workspace  The workspace.
     * @param project    The project dir.
     * @param extraTasks Any extra Gradle tasks to run whilst extracting.
     * @return The new module.
     */
    Module buildModule(Workspace workspace, Path project, Set<String> extraTasks);

    /**
     * Process the given gradle configuration, producing a set of {@link Dependency Dependencies}.
     *
     * @param rootModule    The root module of the workspace.
     * @param configuration The configuration to process.
     * @return The set of dependencies.
     */
    Set<Dependency> processConfiguration(Module rootModule, ConfigurationData configuration);
}
