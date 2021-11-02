package net.covers1624.wt.api.impl.dependency;

import net.covers1624.wt.api.dependency.SourceSetDependency;
import net.covers1624.wt.api.dependency.WorkspaceModuleDependency;
import net.covers1624.wt.api.workspace.WorkspaceModule;

/**
 * Created by covers1624 on 30/6/19.
 */
public class WorkspaceModuleDependencyImpl extends AbstractDependency implements WorkspaceModuleDependency {

    private WorkspaceModule module;

    public WorkspaceModuleDependencyImpl() {
    }

    WorkspaceModuleDependencyImpl(WorkspaceModuleDependency other) {
        this();
        setModule(other.getModule());
    }

    @Override
    public WorkspaceModuleDependency setExport(boolean export) {
        super.setExport(export);
        return this;
    }

    @Override
    public WorkspaceModule getModule() {
        return module;
    }

    @Override
    public WorkspaceModuleDependency setModule(WorkspaceModule module) {
        this.module = module;
        return this;
    }

    @Override
    public int hashCode() {
        int i = 0;
        i = 31 * i + module.getName().hashCode();
        return i;
    }

    @Override
    public boolean equals(Object obj) {
        if (super.equals(obj)) {
            return true;
        }
        if (!(obj instanceof SourceSetDependency)) {
            return false;
        }
        SourceSetDependency other = (SourceSetDependency) obj;
        return other.getModule().getName().equals(getModule().getName());
    }

    @Override
    public WorkspaceModuleDependency copy() {
        return new WorkspaceModuleDependencyImpl(this);
    }
}
