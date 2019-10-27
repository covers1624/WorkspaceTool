package net.covers1624.wt.event;

import net.covers1624.wt.api.WorkspaceToolContext;
import net.covers1624.wt.api.data.ProjectData;

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
