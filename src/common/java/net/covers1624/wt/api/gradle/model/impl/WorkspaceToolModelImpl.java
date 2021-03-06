package net.covers1624.wt.api.gradle.model.impl;

import net.covers1624.wt.api.gradle.data.ProjectData;
import net.covers1624.wt.api.gradle.model.WorkspaceToolModel;
import net.covers1624.wt.event.VersionedClass;

/**
 * An implementation of {@link WorkspaceToolModel}
 * {@inheritDoc}
 *
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
