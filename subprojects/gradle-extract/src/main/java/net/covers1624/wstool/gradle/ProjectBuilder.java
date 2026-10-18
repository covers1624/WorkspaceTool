package net.covers1624.wstool.gradle;

import net.covers1624.wstool.gradle.api.data.ProjectData;
import org.gradle.api.Project;

/**
 * Project builders are used to extract any core information about the project
 * into the provided {@link ProjectData} tree. Most core gradle data should be
 * deposited here.
 * <p>
 * Project builders are run in two phases, first, {@link #preTransformProject}, all builders
 * are offered all projects to pre-transform them, this can be used to modify project structure,
 * remove dependencies and configurations, and build local state.
 * <p>
 * Next, {@link #buildProjectData} where resolution of gradle objects is permitted. Data should be
 * inserted into {@link ProjectData}.
 * <p>
 * Created by covers1624 on 9/9/23.
 */
public interface ProjectBuilder {

    /**
     * Transform the gradle project, and build any internal data structures.
     *
     * @param project     The project to build from.
     * @param projectData The project data to store into.
     */
    default void preTransformProject(Project project, ProjectData projectData) { }

    /**
     * Build any main project related information into the provided {@link ProjectData}.
     *
     * @param project     The project to build from.
     * @param projectData The project data to store into.
     * @param lookupCache Various caches shared between builders and steps.
     */
    default void buildProjectData(Project project, ProjectData projectData, LookupCache lookupCache) { }
}
