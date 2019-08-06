package net.covers1624.wt.event;

import net.covers1624.wt.api.module.ModuleList;
import net.covers1624.wt.api.script.WorkspaceScript;

/**
 * Called after Modules have been constructed and Dependencies have been processed.
 *
 * Used by the ForgeExtension for FMLCoreMods and TweakClasses in RunConfigurations.
 *
 * Created by covers1624 on 30/6/19.
 */
public class ProcessModulesEvent extends Event {

    public static final EventRegistry<ProcessModulesEvent> REGISTRY = new EventRegistry<>(ProcessModulesEvent.class);

    private final ModuleList moduleList;
    private final WorkspaceScript script;

    public ProcessModulesEvent(ModuleList moduleList, WorkspaceScript script) {
        this.moduleList = moduleList;
        this.script = script;
    }

    public WorkspaceScript getScript() {
        return script;
    }

    public ModuleList getModuleList() {
        return moduleList;
    }
}
