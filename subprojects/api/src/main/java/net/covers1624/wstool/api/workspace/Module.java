package net.covers1624.wstool.api.workspace;

import net.covers1624.wstool.gradle.api.data.ProjectData;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Represents a module/project.
 * <p>
 * Created by covers1624 on 2/3/25.
 */
public interface Module {

    /**
     * @return The root directory of this module.
     */
    Path rootDir();

    /**
     * @return The name of this module.
     */
    String name();

    /**
     * @return The submodules of this module.
     */
    Map<String, ? extends Module> subModules();

    /**
     * @return The source sets within this module.
     */
    Map<String, ? extends SourceSet> sourceSets();

    /**
     * @return A list of paths within this module that should be ignored by the workspace/ide.
     */
    List<Path> excludes();

    /**
     * Create a new submodule from this module.
     *
     * @param path The path to this submodule.
     * @param name The name of this submodule.
     * @return The new submodule.
     */
    Module newSubModule(Path path, String name);

    /**
     * Create a new {@link SourceSet} in this module.
     *
     * @param name The name of the {@link SourceSet}.
     * @return The {@link SourceSet}.
     */
    SourceSet newSourceSet(String name);

    /**
     * Get the Gradle data this module was built from.
     * <p>
     * This is used by frameworks to pull frameworks specific gradle extraction data in.
     *
     * @return The Gradle project data that this module was built from.
     */
    @Nullable
    ProjectData projectData();

    /**
     * Set the project data for this module.
     *
     * @param data The data.
     */
    void setProjectData(ProjectData data);
}
