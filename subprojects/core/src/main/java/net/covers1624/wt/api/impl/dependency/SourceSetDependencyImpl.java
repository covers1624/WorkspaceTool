/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.impl.dependency;

import net.covers1624.wt.api.dependency.SourceSetDependency;
import net.covers1624.wt.api.module.Module;

/**
 * Created by covers1624 on 30/6/19.
 */
public class SourceSetDependencyImpl extends AbstractDependency implements SourceSetDependency {

    private Module module;
    private String sourceSet;

    public SourceSetDependencyImpl() {
    }

    public SourceSetDependencyImpl(Module module, String sourceSet) {
        this();
        setModule(module);
        setSourceSet(sourceSet);
    }

    SourceSetDependencyImpl(SourceSetDependency other) {
        this();
        setModule(other.getModule());
        setSourceSet(other.getSourceSet());
    }

    @Override
    public SourceSetDependency setExport(boolean export) {
        super.setExport(export);
        return this;
    }

    @Override
    public Module getModule() {
        return module;
    }

    @Override
    public String getSourceSet() {
        return sourceSet;
    }

    @Override
    public SourceSetDependency setModule(Module module) {
        this.module = module;
        return this;
    }

    @Override
    public SourceSetDependency setSourceSet(String sourceSet) {
        this.sourceSet = sourceSet;
        return this;
    }

    @Override
    public int hashCode() {
        int i = 0;
        i = 31 * i + module.getName().hashCode();
        i = 31 * i + sourceSet.hashCode();
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
        return other.getModule().getName().equals(getModule().getName())//
                && other.getSourceSet().equals(getSourceSet());
    }

    @Override
    public SourceSetDependency copy() {
        return new SourceSetDependencyImpl(this);
    }
}
