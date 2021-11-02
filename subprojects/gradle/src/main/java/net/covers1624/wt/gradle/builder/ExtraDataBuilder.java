/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.gradle.builder;

import net.covers1624.wt.api.gradle.data.ProjectData;
import net.covers1624.wt.api.gradle.data.PluginData;
import net.covers1624.wt.event.VersionedClass;
import org.gradle.api.Project;

/**
 * Created by covers1624 on 18/6/19.
 */
@VersionedClass (2)
public interface ExtraDataBuilder {

    void preBuild(Project project, PluginData pluginData) throws Exception;

    void build(Project project, ProjectData projectData, ProjectData rootProject) throws Exception;
}
