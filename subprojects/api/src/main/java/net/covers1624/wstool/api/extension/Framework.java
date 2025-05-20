package net.covers1624.wstool.api.extension;

import net.covers1624.wstool.api.Environment;
import net.covers1624.wstool.api.module.Module;
import net.covers1624.wstool.api.module.WorkspaceBuilder;
import net.covers1624.wstool.gradle.api.data.ProjectData;

import java.nio.file.Path;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Created by covers1624 on 20/10/24.
 */
public interface Framework {

    /**
     * Build the framework and its modules.
     * <p>
     * Building a framework may include anything, cloning Git repos, adding
     * dependencies to projects, etc.
     *
     * @param env              The {@link Environment}.
     * @param dataExtractor    A function to extract a {@link ProjectData} from a project path.
     * @param moduleFactory    A function to build a {@link Module} from a {@link ProjectData}.
     * @param workspaceBuilder The {@link WorkspaceBuilder}.
     */
    void buildFrameworks(
            Environment env,
            BiFunction<Path, Set<String>, ProjectData> dataExtractor,
            BiFunction<WorkspaceBuilder, ProjectData, Module> moduleFactory,
            WorkspaceBuilder workspaceBuilder
    );
}
