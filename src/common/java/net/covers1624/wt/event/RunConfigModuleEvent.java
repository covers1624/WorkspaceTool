package net.covers1624.wt.event;

import net.covers1624.wt.api.module.Module;
import net.covers1624.wt.api.module.ModuleList;

/**
 * Called by WorkspaceWriter's to figure out what Module is the root for RunConfigurations.
 *
 * Created by covers1624 on 24/7/19.
 */
public class RunConfigModuleEvent extends ResultEvent<Module> {

    public static final EventRegistry<RunConfigModuleEvent> REGISTRY = new EventRegistry<>(RunConfigModuleEvent.class);

    private final ModuleList moduleList;

    protected RunConfigModuleEvent(ModuleList moduleList) {
        super(false);
        this.moduleList = moduleList;
    }

    public ModuleList getModuleList() {
        return moduleList;
    }
}
