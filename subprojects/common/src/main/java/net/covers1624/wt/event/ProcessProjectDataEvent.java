/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.event;

import net.covers1624.wt.api.WorkspaceToolContext;
import net.covers1624.wt.api.gradle.data.ProjectData;

/**
 * Created by covers1624 on 26/10/19.
 */
public class ProcessProjectDataEvent extends Event {

    public static final EventRegistry<ProcessProjectDataEvent> REGISTRY = new EventRegistry<>(ProcessProjectDataEvent.class);

    private final WorkspaceToolContext context;
    private final ProjectData projectData;

    public ProcessProjectDataEvent(WorkspaceToolContext context, ProjectData projectData) {
        this.context = context;
        this.projectData = projectData;
    }

    public WorkspaceToolContext getContext() {
        return context;
    }

    public ProjectData getProjectData() {
        return projectData;
    }
}
