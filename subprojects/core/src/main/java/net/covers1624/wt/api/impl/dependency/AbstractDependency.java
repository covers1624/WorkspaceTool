/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
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
        setExport(other.getExport());
    }

    @Override
    public boolean getExport() {
        return export;
    }

    @Override
    public Dependency setExport(boolean export) {
        this.export = export;
        return this;
    }
}
