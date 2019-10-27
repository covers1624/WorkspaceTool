package net.covers1624.wt.api.dependency;

import net.covers1624.wt.api.workspace.WorkspaceModule;

/**
 * Created by covers1624 on 8/9/19.
 */
public interface WorkspaceModuleDependency extends Dependency {

    WorkspaceModule getModule();

    WorkspaceModuleDependency setModule(WorkspaceModule module);

    @Override
    WorkspaceModuleDependency setExport(boolean value);

    @Override
    Dependency copy();
}
