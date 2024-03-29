/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.gradle.model.impl;

import net.covers1624.wt.api.event.VersionedClass;
import net.covers1624.wt.api.gradle.data.ProjectData;
import net.covers1624.wt.api.gradle.model.WorkspaceToolModel;

/**
 * An implementation of {@link WorkspaceToolModel}
 * {@inheritDoc}
 * <p>
 * Created by covers1624 on 15/6/19.
 */
@VersionedClass (1)
public class WorkspaceToolModelImpl implements WorkspaceToolModel {

    private final ProjectData projectData;

    public WorkspaceToolModelImpl(ProjectData projectData) {
        this.projectData = projectData;
    }

    @Override
    public ProjectData getProjectData() {
        return projectData;
    }
}
