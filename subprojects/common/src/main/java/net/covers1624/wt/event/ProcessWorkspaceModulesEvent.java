package net.covers1624.wt.event;

import net.covers1624.wt.api.WorkspaceToolContext;

/**
 * Created by covers1624 on 30/6/19.
 */
public class ProcessWorkspaceModulesEvent extends Event {

    public static final EventRegistry<ProcessWorkspaceModulesEvent> REGISTRY = new EventRegistry<>(ProcessWorkspaceModulesEvent.class);

    private final WorkspaceToolContext context;

    public ProcessWorkspaceModulesEvent(WorkspaceToolContext context) {
        this.context = context;
    }

    public WorkspaceToolContext getContext() {
        return context;
    }
}
