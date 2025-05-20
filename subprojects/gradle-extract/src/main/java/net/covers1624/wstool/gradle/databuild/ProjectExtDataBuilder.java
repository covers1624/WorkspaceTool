package net.covers1624.wstool.gradle.databuild;

import net.covers1624.wstool.gradle.LookupCache;
import net.covers1624.wstool.gradle.ProjectBuilder;
import net.covers1624.wstool.gradle.api.data.ProjectData;
import net.covers1624.wstool.gradle.api.data.ProjectExtData;
import org.gradle.api.Project;

/**
 * Created by covers1624 on 16/5/23.
 */
public class ProjectExtDataBuilder implements ProjectBuilder {

    @Override
    public void buildProjectData(Project project, ProjectData projectData, LookupCache lookupCache) {
        ProjectExtData data = new ProjectExtData();
        project.getExtensions().getExtraProperties().getProperties().forEach((k, v) -> {
            if (v instanceof CharSequence) {
                data.properties.put(k, v.toString());
            }
        });
        projectData.putData(ProjectExtData.class, data);
    }
}
