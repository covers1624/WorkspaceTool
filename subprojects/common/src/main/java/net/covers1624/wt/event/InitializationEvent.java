package net.covers1624.wt.event;

import net.covers1624.wt.api.WorkspaceToolContext;

/**
 * Called at the beginning of WorkspaceTool's LifeCycle to add any extra functionality to
 * WorkspaceTool subsystems.
 *
 * Created by covers1624 on 30/6/19.
 */
public class InitializationEvent extends Event {

    public static final EventRegistry<InitializationEvent> REGISTRY = new EventRegistry<>(InitializationEvent.class);

    private final WorkspaceToolContext context;

    public InitializationEvent(WorkspaceToolContext context) {
        this.context = context;
    }

    public WorkspaceToolContext getContext() {
        return context;
    }
}
