package net.covers1624.wstool.gradle;

import net.covers1624.wstool.gradle.api.data.PluginData;
import net.covers1624.wstool.gradle.api.data.ProjectData;
import org.gradle.api.Project;

/**
 * Created by covers1624 on 16/5/23.
 */
public interface DataBuilder {

    default void buildPluginData(Project project, PluginData pluginData) { }

    default void buildProjectData(Project project, ProjectData projectData) { }
}
