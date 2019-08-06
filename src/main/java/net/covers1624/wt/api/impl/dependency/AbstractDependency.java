package net.covers1624.wt.api.impl.dependency;

import net.covers1624.wt.api.dependency.Dependency;

/**
 * Created by covers1624 on 22/7/19.
 */
public abstract class AbstractDependency implements Dependency {

    private boolean export = true;

    public AbstractDependency() {

    }

    public AbstractDependency(Dependency other) {
        setExport(other.isExport());
    }

    @Override
    public boolean isExport() {
        return export;
    }

    @Override
    public Dependency setExport(boolean export) {
        this.export = export;
        return this;
    }
}
