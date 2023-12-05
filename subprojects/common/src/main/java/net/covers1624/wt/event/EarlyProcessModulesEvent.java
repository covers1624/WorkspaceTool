/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
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
public class EarlyProcessModulesEvent extends Event {

    public static final EventRegistry<EarlyProcessModulesEvent> REGISTRY = new EventRegistry<>(EarlyProcessModulesEvent.class);

    private final WorkspaceToolContext context;

    public EarlyProcessModulesEvent(WorkspaceToolContext context) {
        this.context = context;
    }

    public WorkspaceToolContext getContext() {
        return context;
    }
}
