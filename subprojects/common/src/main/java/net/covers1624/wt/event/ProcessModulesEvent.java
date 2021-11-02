package net.covers1624.wt.event;

import net.covers1624.wt.api.WorkspaceToolContext;

/**
 * Called after Modules have been constructed and Dependencies have been processed.
 *
 * Used by the ForgeExtension for FMLCoreMods and TweakClasses in RunConfigurations.
 *
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
