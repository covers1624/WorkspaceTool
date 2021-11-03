/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.event;

import net.covers1624.wt.api.WorkspaceToolContext;

/**
 * Called at the beginning of WorkspaceTool's LifeCycle to add any extra functionality to
 * WorkspaceTool subsystems.
 * <p>
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
