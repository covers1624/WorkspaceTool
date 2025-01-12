package net.covers1624.wstool.gradle;

import net.covers1624.wstool.gradle.api.data.PluginData;
import net.covers1624.wstool.gradle.api.data.ProjectData;
import org.gradle.api.Project;

/**
 * Project transformers are run after the project tree has been built, but
 * before any {@link ProjectBuilder}s have been called. They may access a project's {@link PluginData}
 * if they wish to conditionally do things.
 * <p>
 * Transformers can do anything they wish to the project permitted by the Gradle api.
 * <p>
 * Created by covers1624 on 1/12/25.
 */
public interface ProjectTransformer {

    /**
     * Perform some arbitrary transformation to a project.
     *
     * @param project     The project.
     * @param projectData The incomplete {@link ProjectData}.
     */
    void transformProject(Project project, ProjectData projectData);
}
