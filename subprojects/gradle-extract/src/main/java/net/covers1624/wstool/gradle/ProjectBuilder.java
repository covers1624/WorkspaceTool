package net.covers1624.wstool.gradle;

import net.covers1624.wstool.gradle.api.data.ProjectData;
import org.gradle.api.Project;

/**
 * Created by covers1624 on 9/9/23.
 */
public interface ProjectBuilder {

    void buildProjectData(Project project, ProjectData projectData, LookupCache lookupCache);
}
