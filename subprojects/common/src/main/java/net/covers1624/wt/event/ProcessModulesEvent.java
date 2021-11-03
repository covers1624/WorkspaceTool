/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.event;

import net.covers1624.wt.api.WorkspaceToolContext;

/**
 * Called after Modules have been constructed and Dependencies have been processed.
 * <p>
 * Used by the ForgeExtension for FMLCoreMods and TweakClasses in RunConfigurations.
 * <p>
 * Created by covers1624 on 30/6/19.
 */
public class ProcessModulesEvent extends Event {

    public static final EventRegistry<ProcessModulesEvent> REGISTRY = new EventRegistry<>(ProcessModulesEvent.class);

    private final WorkspaceToolContext context;

    public ProcessModulesEvent(WorkspaceToolContext context) {
        this.context = context;
    }

    public WorkspaceToolContext getContext() {
        return context;
    }
}
