package net.covers1624.wt.api.workspace;

import net.covers1624.wt.api.module.ModuleList;
import net.covers1624.wt.api.script.runconfig.RunConfig;

import java.util.Map;

/**
 * Used to write the current Module model out to a Workspace for any IDE.
 *
 * Created by covers1624 on 20/7/19.
 */
public interface WorkspaceWriter<T extends Workspace> {

    /**
     * Write out the workspace.
     *
     * @param frameworkImpl The Framework Impl provided from the script.
     * @param moduleList    The ModuleList.
     * @param runConfigs    The RunConfigurations.
     */
    void write(T frameworkImpl, ModuleList moduleList, Map<String, RunConfig> runConfigs);
}
