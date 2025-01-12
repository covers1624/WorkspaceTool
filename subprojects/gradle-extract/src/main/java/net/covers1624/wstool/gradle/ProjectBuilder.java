package net.covers1624.wstool.gradle;

import net.covers1624.wstool.gradle.api.data.ProjectData;
import org.gradle.api.Project;

/**
 * Project builders are used to extract any core information about the project
 * into the provided {@link ProjectData} tree. Most core gradle data should be
 * deposited here.
 * <p>
 * Project builders are run one-by-one over the entire tree at once, before continuing to
 * the next builder.
 * <p>
 * Created by covers1624 on 9/9/23.
 */
public interface ProjectBuilder {

    /**
     * Build any main project related information into the provided {@link ProjectData}.
     *
     * @param project     The project to build from.
     * @param projectData The project data to store into.
     * @param lookupCache Various caches shared between builders and steps.
     */
    void buildProjectData(Project project, ProjectData projectData, LookupCache lookupCache);
}
